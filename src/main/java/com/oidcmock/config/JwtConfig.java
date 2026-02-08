package com.oidcmock.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

/**
 * Configuration for JWT signing keys.
 * 
 * <p>Generates an RSA keypair on startup for signing JWTs.
 * The key is ephemeral — it changes on every restart.</p>
 * 
 * <p><strong>Note:</strong> This is intentional for a mock server.
 * Production IDPs would load keys from a secure key store.</p>
 */
@Configuration
public class JwtConfig {
    
    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);
    private static final int KEY_SIZE = 2048;
    
    /**
     * Generates an RSA keypair for JWT signing.
     * 
     * <p>The key includes a random key ID (kid) for JWKS identification.</p>
     * 
     * @return RSA keypair for signing and verification
     * @throws JOSEException if key generation fails
     */
    @Bean
    public RSAKey rsaKey() throws JOSEException {
        String keyId = UUID.randomUUID().toString();
        log.info("Generated RSA signing key with kid: {}", keyId);
        
        return new RSAKeyGenerator(KEY_SIZE)
            .keyID(keyId)
            .generate();
    }
}
