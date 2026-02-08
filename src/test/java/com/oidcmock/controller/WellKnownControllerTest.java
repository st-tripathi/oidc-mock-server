package com.oidcmock.controller;

import com.oidcmock.config.OidcProperties;
import com.oidcmock.service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WellKnownController.class)
class WellKnownControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OidcProperties properties;

    @MockBean
    private TokenService tokenService;

    @Test
    void testOpenIdConfiguration() throws Exception {
        given(properties.getIssuer()).willReturn("http://localhost:8080");

        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value("http://localhost:8080"))
                .andExpect(jsonPath("$.authorization_endpoint").value("http://localhost:8080/authorize"));
    }
}
