package com.oidcmock.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Validates the OIDC configuration on startup and logs helpful warnings
 * if common configuration mistakes are detected.
 */
@Component
public class StartupValidator {

    private static final Logger log = LoggerFactory.getLogger(StartupValidator.class);

    private final OidcProperties properties;

    public StartupValidator(OidcProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void validate() {
        log.info("╔════════════════════════════════════════════════════╗");
        log.info("║           OIDC Mock Server Starting up...           ║");
        log.info("╚════════════════════════════════════════════════════╝");

        log.info("Issuer URL: {}", properties.getIssuer());

        if (properties.getUsers() == null || properties.getUsers().isEmpty()) {
            log.warn("⚠️  WARNING: No users configured! Authentication will fail.");
            log.warn("   Add users to application.yaml or mount users.yaml to /app/users.yaml");
        } else {
            log.info("Configured Users: {}", properties.getUsers().size());
        }

        if (properties.getClients() == null || properties.getClients().isEmpty()) {
            log.warn("⚠️  WARNING: No clients configured! Authorization flows will fail.");
            log.warn("   Add clients to application.yaml under oidc.clients");
        } else {
            log.info("Registered Clients: {}", properties.getClients().size());
        }

        log.info("Access Token Expiry: {} seconds", properties.getAccessTokenExpiry());
        log.info("Auth Code Expiry: {} seconds", properties.getAuthCodeExpiry());

        log.info("OIDC Mock Server is READY for local development and testing.");
    }
}
