package com.oidcmock.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OAuth2 token response as per RFC 6749.
 * 
 * @see <a href="https://tools.ietf.org/html/rfc6749#section-5.1">RFC 6749 §5.1</a>
 */
public record TokenResponse(
    @JsonProperty("access_token")
    String accessToken,
    
    @JsonProperty("token_type")
    String tokenType,
    
    @JsonProperty("expires_in")
    long expiresIn,
    
    @JsonProperty("refresh_token")
    String refreshToken,
    
    @JsonProperty("id_token")
    String idToken,
    
    @JsonProperty("scope")
    String scope
) {
    /**
     * Creates a standard Bearer token response.
     */
    public static TokenResponse bearer(
            String accessToken, 
            String idToken, 
            String refreshToken,
            long expiresIn,
            String scope) {
        return new TokenResponse(
            accessToken,
            "Bearer",
            expiresIn,
            refreshToken,
            idToken,
            scope
        );
    }
}
