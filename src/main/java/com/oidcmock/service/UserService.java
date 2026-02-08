package com.oidcmock.service;

import com.oidcmock.config.OidcProperties;
import com.oidcmock.model.User;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for user lookup and authentication.
 * 
 * <p>Users are loaded from configuration (application.yaml).
 * This is a mock implementation — production would use a database.</p>
 */
@Service
public class UserService {
    
    private final OidcProperties properties;
    
    public UserService(OidcProperties properties) {
        this.properties = properties;
    }
    
    /**
     * Finds a user by username.
     * 
     * @param username the username to look up
     * @return the user if found
     */
    public Optional<User> findByUsername(String username) {
        return properties.findUser(username);
    }
    
    /**
     * Validates user credentials.
     * 
     * <p><strong>Note:</strong> This uses plaintext password comparison.
     * This is intentional for a mock server — production would use bcrypt.</p>
     * 
     * @param username the username
     * @param password the password to validate
     * @return the authenticated user if credentials are valid
     */
    public Optional<User> authenticate(String username, String password) {
        return findByUsername(username)
            .filter(user -> user.password().equals(password));
    }
}
