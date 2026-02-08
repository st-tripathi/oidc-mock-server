package com.oidcmock.controller;

import com.oidcmock.service.AuthCodeService;
import com.oidcmock.config.OidcProperties;
import com.oidcmock.model.User;
import com.oidcmock.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

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

    private final UserService userService;
    private final AuthCodeService authCodeService;
    private final OidcProperties properties;

    public AuthorizationController(
            UserService userService,
            AuthCodeService authCodeService,
            OidcProperties properties) {
        this.userService = userService;
        this.authCodeService = authCodeService;
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
            Model model) {

        // Basic validation
        if (!"code".equals(responseType)) {
            return "redirect:" + errorRedirect(redirectUri, "unsupported_response_type", state);
        }

        // Security: Validate client and redirect URI
        if (!isValidRedirect(clientId, redirectUri)) {
            model.addAttribute("error", "Invalid client_id or redirect_uri");
            return "login";
        }

        // Pass params to view
        model.addAttribute("clientId", clientId);
        model.addAttribute("redirectUri", redirectUri);
        model.addAttribute("scope", scope != null ? scope : "openid");
        model.addAttribute("state", state);
        model.addAttribute("nonce", nonce);

        return "login"; // Renders src/main/resources/templates/login.html
    }

    @PostMapping("/authorize")
    public String performLogin(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("scope") String scope,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "nonce", required = false) String nonce,
            Model model) {

        // Security: Validate client and redirect URI (Double check)
        if (!isValidRedirect(clientId, redirectUri)) {
            model.addAttribute("error", "Invalid client_id or redirect_uri");
            return "login";
        }

        Optional<User> user = userService.authenticate(username, password);

        if (user.isPresent()) {
            String code = authCodeService.generateCode(
                    user.get().username(),
                    clientId,
                    redirectUri,
                    scope,
                    nonce,
                    properties.getAuthCodeExpiry());

            String redirectUrl = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("code", code)
                    .queryParamIfPresent("state", Optional.ofNullable(state))
                    .build()
                    .toUriString();

            return "redirect:" + redirectUrl;
        }

        // Login failed
        model.addAttribute("error", "Invalid credentials");
        model.addAttribute("clientId", clientId);
        model.addAttribute("redirectUri", redirectUri);
        model.addAttribute("scope", scope);
        model.addAttribute("state", state);
        model.addAttribute("nonce", nonce);

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
        return properties.findClient(clientId)
                .map(client -> client.redirectUris().contains(redirectUri))
                .orElse(false);
    }
}
