package com.oidcmock.controller;

import com.oidcmock.config.OidcProperties;
import com.oidcmock.model.User;
import com.oidcmock.service.AuthCodeService;
import com.oidcmock.service.TokenService;
import com.oidcmock.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TokenController.class)
class TokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthCodeService authCodeService;

    @MockBean
    private OidcProperties properties;

    @Test
    void testClientCredentialsGrant() throws Exception {
        OidcProperties.Client client = new OidcProperties.Client("demo-client", "demo-secret",
                List.of("http://localhost:8082/login/oauth2/code/oidc"));
        given(properties.findClient("demo-client")).willReturn(Optional.of(client));
        given(tokenService.generateAccessToken(any(), anyString())).willReturn("access-token-123");
        given(tokenService.generateIdToken(any(), eq("demo-client"), any())).willReturn("id-token-123");
        given(tokenService.generateRefreshToken(any())).willReturn("refresh-token-123");
        given(properties.getAccessTokenExpiry()).willReturn(3600L);

        mockMvc.perform(post("/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "client_credentials")
                .param("client_id", "demo-client"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("access-token-123"))
                .andExpect(jsonPath("$.token_type").value("Bearer"));
    }

    @Test
    void testPasswordGrant() throws Exception {
        User user = new User("demo-user", "{noop}password", Map.of("sub", "user-demo"));
        given(userService.authenticate("demo-user", "password")).willReturn(Optional.of(user));
        given(tokenService.generateAccessToken(any(), anyString())).willReturn("access-token-pw");
        given(properties.getAccessTokenExpiry()).willReturn(3600L);

        mockMvc.perform(post("/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "password")
                .param("username", "demo-user")
                .param("password", "password")
                .param("client_id", "demo-client"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("access-token-pw"));
    }

    @Test
    void testClientCredentialsGrantWithBasicAuth() throws Exception {
        OidcProperties.Client client = new OidcProperties.Client("demo-client", "demo-secret",
                List.of("http://localhost:8082/login/oauth2/code/oidc"));
        given(properties.findClient("demo-client")).willReturn(Optional.of(client));
        given(tokenService.generateAccessToken(any(), anyString())).willReturn("access-token-basic");
        given(properties.getAccessTokenExpiry()).willReturn(3600L);

        // Basic Auth for demo-client:demo-secret
        String authHeader = "Basic ZGVtby1jbGllbnQ6ZGVtby1zZWNyZXQ=";

        mockMvc.perform(post("/token")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "client_credentials"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("access-token-basic"));
    }
}
