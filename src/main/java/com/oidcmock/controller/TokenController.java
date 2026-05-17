package com.oidcmock.controller;

import com.nimbusds.jwt.JWTClaimsSet;
import com.oidcmock.config.OidcProperties;
import com.oidcmock.model.TokenResponse;
import com.oidcmock.model.User;
import com.oidcmock.service.AuthCodeService;
import com.oidcmock.service.AuthCodeService.AuthCodeData;
import com.oidcmock.service.TokenService;
import com.oidcmock.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for the OAuth2 Token endpoint.
 * 
 * <p>
 * Implements token exchange as per RFC 6749 §3.2.
 * </p>
 * 
 * <p>
 * Supported grant types:
 * </p>
 * <ul>
 * <li>{@code authorization_code} - Exchange auth code for tokens</li>
 * <li>{@code password} - Direct username/password (for testing)</li>
 * <li>{@code refresh_token} - Refresh access token</li>
 * </ul>
 * 
 * @see <a href="https://tools.ietf.org/html/rfc6749#section-3.2">RFC 6749
 *      §3.2</a>
 */
@RestController
public class TokenController {

    private static final Logger log = LoggerFactory.getLogger(TokenController.class);

    private final TokenService tokenService;
    private final UserService userService;
    private final AuthCodeService authCodeService;
    private final OidcProperties properties;

    public TokenController(
            TokenService tokenService,
            UserService userService,
            AuthCodeService authCodeService,
            OidcProperties properties) {
        this.tokenService = tokenService;
        this.userService = userService;
        this.authCodeService = authCodeService;
        this.properties = properties;
    }

    /**
     * Token endpoint - exchanges credentials for tokens.
     */
    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> token(
            @RequestParam("grant_type") String grantType,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            @RequestParam(value = "client_id", required = false) String clientId,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "refresh_token", required = false) String refreshToken,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "client_secret", required = false) String clientSecretParam,
            @RequestParam(value = "code_verifier", required = false) String codeVerifier,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("Token request: grant_type={}, clientId={}, code={}, redirectUri={}, authHeader={}",
                grantType, clientId, code, redirectUri, authHeader != null ? "present" : "absent");

        String effectiveClientId = clientId;
        String effectiveClientSecret = clientSecretParam;

        // Basic Auth takes precedence over request params for client authentication
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            try {
                String base64Credentials = authHeader.substring(6);
                String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
                // credentials = "client_id:client_secret"
                String[] parts = credentials.split(":", 2);
                if (effectiveClientId == null) {
                    effectiveClientId = parts[0];
                }
                if (parts.length == 2) {
                    effectiveClientSecret = parts[1];
                }
                log.debug("Extracted clientId from Basic Auth: {}", effectiveClientId);
            } catch (Exception e) {
                log.error("Failed to decode Basic Auth header", e);
            }
        }

        return switch (grantType) {
            case "authorization_code" -> handleAuthorizationCode(code, redirectUri, effectiveClientId, codeVerifier);
            case "password" -> handlePasswordGrant(username, password, effectiveClientId, scope);
            case "client_credentials" -> handleClientCredentials(effectiveClientId, effectiveClientSecret, scope);
            case "refresh_token" -> handleRefreshToken(refreshToken);
            default -> errorResponse("unsupported_grant_type", "Grant type not supported: " + grantType);
        };
    }

    private ResponseEntity<?> handleAuthorizationCode(String code, String redirectUri, String clientId, String codeVerifier) {
        if (code == null || redirectUri == null || clientId == null) {
            return errorResponse("invalid_request", "Missing required parameters");
        }

        Optional<AuthCodeData> codeData = authCodeService.exchangeCode(code, clientId, redirectUri, codeVerifier);

        if (codeData.isEmpty()) {
            return errorResponse("invalid_grant", "Invalid or expired authorization code");
        }

        AuthCodeData data = codeData.get();
        Optional<User> user = userService.findByUsername(data.subject());

        if (user.isEmpty()) {
            // User was deleted after code was issued
            return errorResponse("invalid_grant", "User not found");
        }

        return issueTokens(user.get(), clientId, data.scope(), data.nonce());
    }

    private ResponseEntity<?> handlePasswordGrant(String username, String password, String clientId, String scope) {
        if (username == null || password == null) {
            return errorResponse("invalid_request", "Username and password required");
        }

        Optional<User> user = userService.authenticate(username, password);

        if (user.isEmpty()) {
            return errorResponse("invalid_grant", "Invalid username or password");
        }

        String effectiveScope = scope != null ? scope : "openid profile email";
        String effectiveClientId = clientId != null ? clientId : "default-client";

        return issueTokens(user.get(), effectiveClientId, effectiveScope, null);
    }

    private ResponseEntity<?> handleClientCredentials(String clientId, String clientSecret, String scope) {
        if (clientId == null) {
            return errorResponse("invalid_client", "Client ID required");
        }

        Optional<OidcProperties.Client> client = properties.findClient(clientId);
        if (client.isEmpty()) {
            return errorResponse("invalid_client", "Unknown client: " + clientId);
        }

        String expectedSecret = client.get().clientSecret();
        if (expectedSecret != null && !expectedSecret.isEmpty()) {
            if (clientSecret == null || !java.security.MessageDigest.isEqual(
                    expectedSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    clientSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                return errorResponse("invalid_client", "Invalid client credentials");
            }
        }

        User clientUser = new User(clientId, "", Map.of("sub", clientId, "roles", List.of("client")));
        String effectiveScope = scope != null ? scope : "openid";

        return issueTokens(clientUser, clientId, effectiveScope, null);
    }

    private ResponseEntity<?> handleRefreshToken(String refreshToken) {
        if (refreshToken == null) {
            return errorResponse("invalid_request", "Refresh token required");
        }

        Optional<JWTClaimsSet> claims = tokenService.validateToken(refreshToken);

        if (claims.isEmpty()) {
            return errorResponse("invalid_grant", "Invalid or expired refresh token");
        }

        JWTClaimsSet tokenClaims = claims.get();
        String subject = tokenClaims.getSubject();
        Optional<User> user = userService.findByUsername(subject);

        if (user.isEmpty()) {
            return errorResponse("invalid_grant", "User not found");
        }

        // Preserve the original client and scope from the refresh token claims
        String originalClientId = (String) tokenClaims.getClaim("client_id");
        String originalScope = (String) tokenClaims.getClaim("scope");
        String effectiveClientId = originalClientId != null ? originalClientId : "default-client";
        String effectiveScope = originalScope != null ? originalScope : "openid profile email";

        return issueTokens(user.get(), effectiveClientId, effectiveScope, null);
    }

    private ResponseEntity<TokenResponse> issueTokens(User user, String clientId, String scope, String nonce) {
        String accessToken = tokenService.generateAccessToken(user, scope);
        String idToken = tokenService.generateIdToken(user, clientId, nonce);
        String newRefreshToken = tokenService.generateRefreshToken(user, clientId, scope);

        TokenResponse response = TokenResponse.bearer(
                accessToken,
                idToken,
                newRefreshToken,
                properties.getAccessTokenExpiry(),
                scope);

        log.info("Issued tokens for user: {}", user.getSubject());
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, String>> errorResponse(String error, String description) {
        log.warn("Token error: {} - {}", error, description);
        return ResponseEntity.badRequest().body(Map.of(
                "error", error,
                "error_description", description));
    }
}
