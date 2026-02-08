package com.oidcmock.config;

import com.oidcmock.model.User;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Configuration properties for OIDC server settings.
 * 
 * <p>Binds to properties under the {@code oidc} prefix in application.yaml.</p>
 */
@Component
@ConfigurationProperties(prefix = "oidc")
public class OidcProperties {
    
    private String issuer = "http://localhost:8080";
    private long accessTokenExpiry = 3600;
    private long idTokenExpiry = 3600;
    private long refreshTokenExpiry = 86400;
    private long authCodeExpiry = 300;
    private List<User> users = new ArrayList<>();
    
    // Getters and Setters
    
    public String getIssuer() {
        return issuer;
    }
    
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
    
    public long getAccessTokenExpiry() {
        return accessTokenExpiry;
    }
    
    public void setAccessTokenExpiry(long accessTokenExpiry) {
        this.accessTokenExpiry = accessTokenExpiry;
    }
    
    public long getIdTokenExpiry() {
        return idTokenExpiry;
    }
    
    public void setIdTokenExpiry(long idTokenExpiry) {
        this.idTokenExpiry = idTokenExpiry;
    }
    
    public long getRefreshTokenExpiry() {
        return refreshTokenExpiry;
    }
    
    public void setRefreshTokenExpiry(long refreshTokenExpiry) {
        this.refreshTokenExpiry = refreshTokenExpiry;
    }
    
    public long getAuthCodeExpiry() {
        return authCodeExpiry;
    }
    
    public void setAuthCodeExpiry(long authCodeExpiry) {
        this.authCodeExpiry = authCodeExpiry;
    }
    
    public List<User> getUsers() {
        return users;
    }
    
    public void setUsers(List<User> users) {
        this.users = users;
    }
    
    /**
     * Finds a user by username.
     */
    public Optional<User> findUser(String username) {
        return users.stream()
            .filter(u -> u.username().equals(username))
            .findFirst();
    }
}
