package com.oidcmock.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
public class OidcFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testAuthorizationCodeFlow_Success() throws Exception {
        // Step 1: Login and get code
        mockMvc.perform(post("/authorize")
                .param("username", "testuser")
                .param("password", "password123")
                .param("client_id", "test-client")
                .param("redirect_uri", "http://localhost:9090/callback")
                .param("response_type", "code")
                .param("scope", "openid profile"))
                .andExpect(status().is3xxRedirection()) // Redirect
                .andExpect(header().string("Location", containsString("code=")));
    }

    @Test
    public void testImplicitFlow_ShouldIncludeIdTokenWhenOpenIdScopePresent() throws Exception {
        // Step 1: Login and get token in fragment
        MvcResult result = mockMvc.perform(post("/authorize")
                .param("username", "testuser")
                .param("password", "password123")
                .param("client_id", "test-client")
                .param("redirect_uri", "http://localhost:9090/callback")
                .param("response_type", "token")
                .param("scope", "openid profile")
                .param("nonce", "xyz123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String location = result.getResponse().getHeader("Location");
        org.junit.jupiter.api.Assertions.assertTrue(location.contains("access_token="));
        org.junit.jupiter.api.Assertions.assertTrue(location.contains("id_token="));
        org.junit.jupiter.api.Assertions.assertTrue(location.contains("token_type=Bearer"));
    }
}
