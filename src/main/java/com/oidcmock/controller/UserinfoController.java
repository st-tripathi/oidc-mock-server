package com.oidcmock.controller;

import com.nimbusds.jwt.JWTClaimsSet;
import com.oidcmock.service.TokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller for the OIDC UserInfo endpoint.
 * 
 * <p>
 * Returns claims about the authenticated user based on the access token.
 * </p>
 * 
 * @see <a href=
 *      "https://openid.net/specs/openid-connect-core-1_0.html#UserInfo">OIDC
 *      Core §5.3</a>
 */
@RestController
public class UserinfoController {

    // JWT registered claim names that must not be returned as identity claims
    private static final Set<String> JWT_INTERNAL_CLAIMS = Set.of(
            "iss", "aud", "iat", "exp", "nbf", "jti", "token_type", "scope", "auth_time");

    private final TokenService tokenService;

    public UserinfoController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    /**
     * Returns user claims for the given access token.
     * 
     * @param authHeader Authorization header containing "Bearer <token>"
     * @return JSON map of user claims
     */
    @GetMapping(value = "/userinfo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> userinfo(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header("WWW-Authenticate", "Bearer error=\"invalid_token\"")
                    .build();
        }

        String token = authHeader.substring(7);
        Optional<JWTClaimsSet> claims = tokenService.validateToken(token);

        if (claims.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header("WWW-Authenticate", "Bearer error=\"invalid_token\"")
                    .build();
        }

        Map<String, Object> identityClaims = claims.get().getClaims().entrySet().stream()
                .filter(e -> !JWT_INTERNAL_CLAIMS.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return ResponseEntity.ok(identityClaims);
    }
}
