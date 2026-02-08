package com.oidcmock.model;

import java.util.List;
import java.util.Map;

/**
 * Represents a configurable test user.
 * 
 * <p>
 * Users are loaded from application.yaml and used for authentication
 * and populating token claims.
 * </p>
 * 
 * @param username The login username
 * @param password The plaintext password (for testing only)
 * @param claims   OIDC claims to include in tokens
 */
public record User(
        String username,
        String password,
        Map<String, Object> claims) {
    /**
     * Returns the subject claim (user identifier).
     */
    public String getSubject() {
        if (claims != null && claims.containsKey("sub")) {
            Object sub = claims.get("sub");
            if (sub != null) {
                return sub.toString();
            }
        }
        return username;
    }

    /**
     * Returns the email claim if present.
     */
    public String getEmail() {
        if (claims != null && claims.containsKey("email")) {
            Object email = claims.get("email");
            if (email != null) {
                return email.toString();
            }
        }
        return null;
    }

    /**
     * Returns the roles claim if present.
     */
    @SuppressWarnings("unchecked")
    public List<String> getRoles() {
        if (claims != null && claims.containsKey("roles")) {
            Object roles = claims.get("roles");
            if (roles instanceof List) {
                return (List<String>) roles;
            }
        }
        return List.of();
    }
}
