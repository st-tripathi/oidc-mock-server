package com.oidcmock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * OIDC Mock Server Application.
 * 
 * <p>A lightweight OAuth2/OIDC mock server for local development and testing.
 * Implements core OIDC endpoints without external dependencies.</p>
 * 
 * @see <a href="https://openid.net/specs/openid-connect-core-1_0.html">OIDC Core Spec</a>
 */
@SpringBootApplication
public class OidcMockServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OidcMockServerApplication.class, args);
    }
}
