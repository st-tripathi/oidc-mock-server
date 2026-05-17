package com.oidcmock.controller;

import com.oidcmock.service.AuthCodeService;
import com.oidcmock.config.OidcProperties;
import com.oidcmock.model.User;
import com.oidcmock.service.TokenService;
import com.oidcmock.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for the OAuth2 Authorization endpoint.
 * 
 * <p>
 * Handles the interactive login flow:
 * </p>
 * <ol>
 * <li>{@code GET /authorize} - Shows login form</li>
 * <li>{@code POST /authorize} - Validates credentials and redirects with
 * code</li>
 * </ol>
 * 
 * @see <a href="https://tools.ietf.org/html/rfc6749#section-3.1">RFC 6749
 *      §3.1</a>
 */
@Controller
public class AuthorizationController {
    private static final Logger log = LoggerFactory.getLogger(AuthorizationController.class);

    private final UserService userService;
    private final AuthCodeService authCodeService;
    private final TokenService tokenService;
    private final OidcProperties properties;

    public AuthorizationController(
            UserService userService,
            AuthCodeService authCodeService,
            TokenService tokenService,
            OidcProperties properties) {
        this.userService = userService;
        this.authCodeService = authCodeService;
        this.tokenService = tokenService;
        this.properties = properties;
    }

    @GetMapping("/authorize")
    public String authorize(
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("response_type") String responseType,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "nonce", required = false) String nonce,
            @RequestParam(value = "code_challenge", required = false) String codeChallenge,
            @RequestParam(value = "code_challenge_method", required = false) String codeChallengeMethod,
            Model model) {

        // Security: Validate client and redirect URI FIRST — before using redirectUri in any redirect
        if (!isValidRedirect(clientId, redirectUri)) {
            log.warn("Invalid authorize request: clientId={}, redirectUri={}", clientId, redirectUri);

            String availableClients = properties.getClients().stream()
                    .map(OidcProperties.Client::clientId)
                    .collect(java.util.stream.Collectors.joining(", "));

            model.addAttribute("error", String.format(
                    "Invalid client_id '%s' or redirect_uri '%s'. Registered clients: [%s]",
                    clientId, redirectUri, availableClients));
            return "login";
        }

        // Now that redirectUri is validated, it is safe to use it in error redirects
        if (!"code".equals(responseType) && !"token".equals(responseType)) {
            return "redirect:" + errorRedirect(redirectUri, "unsupported_response_type", state);
        }

        // Pass params to view
        model.addAttribute("clientId", clientId);
        model.addAttribute("redirectUri", redirectUri);
        model.addAttribute("scope", scope != null ? scope : "openid");
        model.addAttribute("state", state);
        model.addAttribute("nonce", nonce);
        model.addAttribute("responseType", responseType);
        model.addAttribute("codeChallenge", codeChallenge);
        model.addAttribute("codeChallengeMethod", codeChallengeMethod != null ? codeChallengeMethod : "plain");

        return "login"; // Renders src/main/resources/templates/login.html
    }

    @PostMapping("/authorize")
    public String performLogin(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("response_type") String responseType,
            @RequestParam("scope") String scope,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "nonce", required = false) String nonce,
            @RequestParam(value = "code_challenge", required = false) String codeChallenge,
            @RequestParam(value = "code_challenge_method", required = false) String codeChallengeMethod,
            Model model) {

        // Security: Validate client and redirect URI (Double check)
        if (!isValidRedirect(clientId, redirectUri)) {
            model.addAttribute("error", "Invalid client_id or redirect_uri");
            return "login";
        }

        Optional<User> user = userService.authenticate(username, password);

        if (user.isPresent()) {
            log.info("User authenticated: {} for flow: {}", username, responseType);

            if ("token".equals(responseType)) {
                // Implicit Flow: Redirect with token in fragment
                String accessToken = tokenService.generateAccessToken(user.get(), scope);

                StringBuilder fragment = new StringBuilder("access_token=").append(accessToken);
                fragment.append("&token_type=Bearer");
                fragment.append("&expires_in=").append(properties.getAccessTokenExpiry());

                // OIDC Spec: Include id_token if openid scope is present
                if (scope != null && scope.contains("openid")) {
                    String idToken = tokenService.generateIdToken(user.get(), clientId, nonce);
                    fragment.append("&id_token=").append(idToken);
                }

                if (state != null) {
                    fragment.append("&state=").append(UriUtils.encode(state, StandardCharsets.UTF_8));
                }

                return "redirect:" + redirectUri + "#" + fragment.toString();
            }

            // Authorization Code Flow
            String code = authCodeService.generateCode(
                    user.get().username(),
                    clientId,
                    redirectUri,
                    scope,
                    nonce,
                    properties.getAuthCodeExpiry(),
                    codeChallenge,
                    codeChallengeMethod);

            String redirectUrl = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("code", code)
                    .queryParamIfPresent("state", Optional.ofNullable(state))
                    .build()
                    .toUriString();

            return "redirect:" + redirectUrl;
        }

        log.warn("Authentication failed for user: {}", username);

        // Login failed
        String availableUsers = properties.getUsers().stream()
                .map(User::username)
                .collect(java.util.stream.Collectors.joining(", "));

        model.addAttribute("error", String.format("Invalid credentials. Available users: [%s]", availableUsers));
        model.addAttribute("clientId", clientId);
        model.addAttribute("redirectUri", redirectUri);
        model.addAttribute("scope", scope);
        model.addAttribute("state", state);
        model.addAttribute("nonce", nonce);
        model.addAttribute("codeChallenge", codeChallenge);
        model.addAttribute("codeChallengeMethod", codeChallengeMethod);

        return "login";
    }

    private String errorRedirect(String redirectUri, String error, String state) {
        return UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("error", error)
                .queryParamIfPresent("state", Optional.ofNullable(state))
                .build()
                .toUriString();
    }

    private boolean isValidRedirect(String clientId, String redirectUri) {
        String normalizedRequest = redirectUri.endsWith("/") ? redirectUri.substring(0, redirectUri.length() - 1)
                : redirectUri;

        return properties.findClient(clientId)
                .map(client -> client.redirectUris().stream()
                        .map(uri -> uri.endsWith("/") ? uri.substring(0, uri.length() - 1) : uri)
                        .anyMatch(normalizedRequest::equals))
                .orElse(false);
    }
}
