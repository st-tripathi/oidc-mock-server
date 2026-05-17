package com.oidcmock.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.oidcmock.config.OidcProperties;
import com.oidcmock.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {

    private TokenService tokenService;
    private OidcProperties properties;

    @BeforeEach
    void setUp() throws JOSEException {
        properties = new OidcProperties();
        properties.setIssuer("http://test-issuer");
        properties.setAccessTokenExpiry(60); // 1 minute

        var rsaKey = new RSAKeyGenerator(2048)
                .keyID("test-key")
                .generate();

        tokenService = new TokenService(rsaKey, properties);
    }

    @Test
    void testGenerateAndValidateAccessToken() {
        User user = new User("testuser", "pass", Map.of("roles", List.of("admin")));

        String token = tokenService.generateAccessToken(user, "openid");
        assertNotNull(token);

        Optional<JWTClaimsSet> claims = tokenService.validateToken(token);
        assertTrue(claims.isPresent());

        assertEquals("testuser", claims.get().getSubject());
        assertEquals("http://test-issuer", claims.get().getIssuer());
        assertEquals("openid", claims.get().getClaim("scope"));
    }

    @Test
    void testRefreshTokenPreservesClientIdAndScope() throws Exception {
        properties.setRefreshTokenExpiry(300);
        User user = new User("testuser", "pass", Map.of("sub", "user-001"));

        String refreshToken = tokenService.generateRefreshToken(user, "my-client", "openid profile");
        assertNotNull(refreshToken);

        Optional<JWTClaimsSet> claims = tokenService.validateToken(refreshToken);
        assertTrue(claims.isPresent());

        assertEquals("user-001", claims.get().getSubject());
        assertEquals("my-client", claims.get().getClaim("client_id"));
        assertEquals("openid profile", claims.get().getClaim("scope"));
        assertEquals("refresh_token", claims.get().getClaim("token_type"));
    }
}
