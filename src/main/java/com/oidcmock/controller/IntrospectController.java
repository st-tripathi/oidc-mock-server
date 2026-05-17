package com.oidcmock.controller;

import com.nimbusds.jwt.JWTClaimsSet;
import com.oidcmock.service.TokenService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Token introspection endpoint as per RFC 7662.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7662">RFC 7662</a>
 */
@RestController
public class IntrospectController {

    private final TokenService tokenService;

    public IntrospectController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping(value = "/introspect",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> introspect(
            @RequestParam("token") String token) {

        Optional<JWTClaimsSet> claims = tokenService.validateToken(token);

        if (claims.isEmpty()) {
            return ResponseEntity.ok(Map.of("active", false));
        }

        JWTClaimsSet c = claims.get();
        Map<String, Object> response = new HashMap<>();
        response.put("active", true);
        response.put("sub", c.getSubject());
        response.put("iss", c.getIssuer());
        response.put("iat", c.getIssueTime() != null ? c.getIssueTime().getTime() / 1000 : null);
        response.put("exp", c.getExpirationTime() != null ? c.getExpirationTime().getTime() / 1000 : null);
        if (c.getClaim("scope") != null)    response.put("scope", c.getClaim("scope"));
        if (c.getClaim("client_id") != null) response.put("client_id", c.getClaim("client_id"));
        if (c.getClaim("token_type") != null) response.put("token_use", c.getClaim("token_type"));
        if (!c.getAudience().isEmpty())      response.put("aud", c.getAudience());

        return ResponseEntity.ok(response);
    }
}
