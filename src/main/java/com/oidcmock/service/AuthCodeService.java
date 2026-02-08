package com.oidcmock.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing authorization codes.
 * 
 * <p>Authorization codes are short-lived, single-use tokens exchanged for
 * access tokens. This implementation uses an in-memory store with TTL expiry.</p>
 * 
 * <p><strong>Note:</strong> State is lost on restart. This is acceptable for
 * a mock server but not for production.</p>
 */
@Service
public class AuthCodeService {

    private static final Logger log = LoggerFactory.getLogger(AuthCodeService.class);
    private static final int CODE_LENGTH = 32;
    private static final SecureRandom RANDOM = new SecureRandom();
    
    /**
     * Stored authorization code data.
     */
    public record AuthCodeData(
        String subject,
        String clientId,
        String redirectUri,
        String scope,
        String nonce,
        Instant expiresAt
    ) {
        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
    
    private final Map<String, AuthCodeData> codes = new ConcurrentHashMap<>();
    
    /**
     * Generates a new authorization code.
     * 
     * @param subject     the user's subject identifier
     * @param clientId    the client requesting authorization
     * @param redirectUri the redirect URI for the callback
     * @param scope       the requested scopes
     * @param nonce       optional nonce for replay protection
     * @param expirySeconds code lifetime in seconds
     * @return the generated authorization code
     */
    public String generateCode(
            String subject,
            String clientId,
            String redirectUri,
            String scope,
            String nonce,
            long expirySeconds) {
        
        String code = generateSecureCode();
        Instant expiresAt = Instant.now().plusSeconds(expirySeconds);
        
        codes.put(code, new AuthCodeData(
            subject, clientId, redirectUri, scope, nonce, expiresAt
        ));
        
        return code;
    }
    
    /**
     * Exchanges an authorization code for its associated data.
     * 
     * <p>The code is consumed (deleted) only on successful exchange.
     * Expired codes are cleaned up, but validation failures do not consume the code.</p>
     * 
     * @param code        the authorization code
     * @param clientId    the client ID (must match original)
     * @param redirectUri the redirect URI (must match original)
     * @return the code data if valid and not expired
     */
    public Optional<AuthCodeData> exchangeCode(
            String code, 
            String clientId, 
            String redirectUri) {
        
        AuthCodeData data = codes.get(code);
        
        if (data == null) {
            return Optional.empty();
        }
        
        if (data.isExpired()) {
            codes.remove(code);
            log.debug("Auth code expired and removed");
            return Optional.empty();
        }
        
        if (!data.clientId().equals(clientId)) {
            log.warn("Client ID mismatch for auth code exchange: expected={}, actual={}", data.clientId(), clientId);
            return Optional.empty();
        }
        
        if (!data.redirectUri().equals(redirectUri)) {
            log.warn("Redirect URI mismatch for auth code exchange: expected={}, actual={}", data.redirectUri(), redirectUri);
            return Optional.empty();
        }
        
        // Only consume the code after successful validation
        codes.remove(code);
        return Optional.of(data);
    }

    /**
     * Periodically cleans up expired authorization codes.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedRate = 300000)
    public void cleanupExpiredCodes() {
        int before = codes.size();
        codes.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int removed = before - codes.size();
        if (removed > 0) {
            log.debug("Cleaned up {} expired auth codes", removed);
        }
    }
    
    private String generateSecureCode() {
        byte[] bytes = new byte[CODE_LENGTH];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
