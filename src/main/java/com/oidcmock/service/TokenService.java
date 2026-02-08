package com.oidcmock.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.oidcmock.config.OidcProperties;
import com.oidcmock.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for JWT token generation and validation.
 * 
 * <p>Generates signed JWTs for access tokens and ID tokens using RS256.
 * Tokens include standard OIDC claims plus any custom claims from user config.</p>
 */
@Service
public class TokenService {
    
    private static final Logger log = LoggerFactory.getLogger(TokenService.class);
    
    private final RSAKey rsaKey;
    private final JWSSigner signer;
    private final OidcProperties properties;
    
    public TokenService(RSAKey rsaKey, OidcProperties properties) throws JOSEException {
        this.rsaKey = rsaKey;
        this.signer = new RSASSASigner(rsaKey);
        this.properties = properties;
    }
    
    /**
     * Generates an access token for the given user.
     * 
     * @param user  the authenticated user
     * @param scope the granted scopes
     * @return signed JWT access token
     */
    public String generateAccessToken(User user, String scope) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(properties.getAccessTokenExpiry());
        
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
            .issuer(properties.getIssuer())
            .subject(user.getSubject())
            .audience(properties.getIssuer())
            .issueTime(Date.from(now))
            .expirationTime(Date.from(expiry))
            .jwtID(UUID.randomUUID().toString())
            .claim("scope", scope)
            .claim("token_type", "access_token");
        
        // Add roles if present
        if (!user.getRoles().isEmpty()) {
            builder.claim("roles", user.getRoles());
        }
        
        return signToken(builder.build());
    }
    
    /**
     * Generates an ID token for the given user.
     * 
     * <p>ID tokens contain identity claims like name, email, etc.</p>
     * 
     * @param user     the authenticated user
     * @param clientId the client ID (audience)
     * @param nonce    optional nonce for replay protection
     * @return signed JWT ID token
     */
    public String generateIdToken(User user, String clientId, String nonce) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(properties.getIdTokenExpiry());
        
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
            .issuer(properties.getIssuer())
            .subject(user.getSubject())
            .audience(clientId)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(expiry))
            .claim("auth_time", Date.from(now));
        
        // Add nonce if provided
        if (nonce != null && !nonce.isEmpty()) {
            builder.claim("nonce", nonce);
        }
        
        // Add user claims from config
        Map<String, Object> claims = user.claims();
        if (claims != null) {
            claims.forEach((key, value) -> {
                if (!"sub".equals(key)) {  // sub is already set
                    builder.claim(key, value);
                }
            });
        }
        
        return signToken(builder.build());
    }
    
    /**
     * Generates a refresh token.
     * 
     * @param user the authenticated user
     * @return signed JWT refresh token
     */
    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(properties.getRefreshTokenExpiry());
        
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .issuer(properties.getIssuer())
            .subject(user.getSubject())
            .issueTime(Date.from(now))
            .expirationTime(Date.from(expiry))
            .jwtID(UUID.randomUUID().toString())
            .claim("token_type", "refresh_token")
            .build();
        
        return signToken(claims);
    }
    
    /**
     * Validates a token and extracts its claims.
     * 
     * @param token the JWT to validate
     * @return claims if token is valid and not expired
     */
    public Optional<JWTClaimsSet> validateToken(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            
            // Verify signature
            RSASSAVerifier verifier = new RSASSAVerifier(rsaKey.toPublicJWK());
            if (!jwt.verify(verifier)) {
                log.debug("Token signature verification failed");
                return Optional.empty();
            }
            
            // Check expiry
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            if (claims.getExpirationTime().before(new Date())) {
                log.debug("Token expired");
                return Optional.empty();
            }
            
            return Optional.of(claims);
            
        } catch (ParseException | JOSEException e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Returns the public key for JWKS endpoint.
     */
    public RSAKey getPublicKey() {
        return rsaKey.toPublicJWK();
    }
    
    private String signToken(JWTClaimsSet claims) {
        try {
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(rsaKey.getKeyID())
                .build();
            
            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(signer);
            
            return jwt.serialize();
            
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to sign token", e);
        }
    }
}
