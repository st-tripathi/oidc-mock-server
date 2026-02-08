package com.oidcmock.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
public class OpenRedirectTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testOpenRedirectVulnerability_ShouldRejectUnknownHost() throws Exception {
        // Attack scenario: Attacker provides a malicious redirect_uri
        String evilUri = "http://evil.com/callback";

        // Expectation: The server must reject this because it's not in the allowlist
        // for 'test-client'
        // 'test-client' allowlist in application.yaml:
        // - http://localhost:8080/login/oauth2/code/oidc
        // - http://localhost:9090/callback
        // - http://localhost:3000/api/auth/callback/oidc

        mockMvc.perform(get("/authorize")
                .param("client_id", "test-client")
                .param("response_type", "code")
                .param("redirect_uri", evilUri))
                .andExpect(status().isOk()) // Returns 200 OK (Login Page)
                .andExpect(view().name("login"))
                .andExpect(model().attribute("error", org.hamcrest.Matchers
                        .containsString("Invalid client_id 'test-client' or redirect_uri 'http://evil.com/callback'")));
    }
}
