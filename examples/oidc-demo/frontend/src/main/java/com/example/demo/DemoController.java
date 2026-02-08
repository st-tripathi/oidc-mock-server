package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import java.util.Map;

@Controller
public class DemoController {

    @Value("${kong.url}")
    private String kongUrl;

    private final OAuth2AuthorizedClientService authorizedClientService;

    public DemoController(OAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
    }

    @Value("${spring.security.oauth2.client.provider.oidc.token-uri}")
    private String tokenUri;

    @Value("${spring.security.oauth2.client.registration.oidc.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.oidc.client-secret}")
    private String clientSecret;

    @GetMapping("/")
    public String index(Model model,
            @AuthenticationPrincipal OidcUser principal,
            OAuth2AuthenticationToken authentication) {
        if (principal != null && authentication != null) {
            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                    authentication.getAuthorizedClientRegistrationId(),
                    authentication.getName());

            if (client != null) {
                model.addAttribute("name", principal.getName());
                model.addAttribute("email", principal.getEmail());
                model.addAttribute("claims", principal.getClaims());
                model.addAttribute("accessToken", client.getAccessToken().getTokenValue());
                model.addAttribute("flow", "Authorization Code");

                // Call Backend via Kong
                callBackend(model, client.getAccessToken().getTokenValue());
            }
        }
        return "index";
    }

    @PostMapping("/flow/client-credentials")
    public String clientCredentialsFlow(Model model) {
        try {
            RestClient restClient = RestClient.create();

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "client_credentials");
            formData.add("scope", "openid");

            Map<String, Object> response = restClient.post()
                    .uri(tokenUri)
                    .header("Authorization",
                            "Basic " + java.util.Base64.getEncoder()
                                    .encodeToString((clientId + ":" + clientSecret).getBytes()))
                    .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                    });

            model.addAttribute("name", "System (Client Credentials)");
            model.addAttribute("accessToken", response.get("access-token")); // OIDC Mock Server uses access-token in
                                                                             // JSON by default? Let me check
                                                                             // TokenResponse
            // Wait, let's check TokenResponse.java or TokenController's issueTokens
            // response.
            // Map keys in issueTokens: access_token, id_token, refresh_token, etc.
            model.addAttribute("accessToken", response.get("access_token"));
            model.addAttribute("flow", "Client Credentials");

            callBackend(model, (String) response.get("access_token"));
        } catch (Exception e) {
            model.addAttribute("error", "Client Credentials Flow failed: " + e.getMessage());
        }
        return "index";
    }

    @PostMapping("/flow/password")
    public String passwordFlow(@RequestParam String username, @RequestParam String password, Model model) {
        try {
            RestClient restClient = RestClient.create();

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "password");
            formData.add("username", username);
            formData.add("password", password);
            formData.add("scope", "openid profile email");

            Map<String, Object> response = restClient.post()
                    .uri(tokenUri)
                    .header("Authorization",
                            "Basic " + java.util.Base64.getEncoder()
                                    .encodeToString((clientId + ":" + clientSecret).getBytes()))
                    .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                    });

            model.addAttribute("name", username + " (Password Grant)");
            model.addAttribute("accessToken", response.get("access_token"));
            model.addAttribute("flow", "Password Grant");

            callBackend(model, (String) response.get("access_token"));
        } catch (Exception e) {
            model.addAttribute("error", "Password Grant Flow failed: " + e.getMessage());
        }
        return "index";
    }

    private void callBackend(Model model, String token) {
        try {
            RestClient restClient = RestClient.create();
            String response = restClient.get()
                    .uri(kongUrl + "/api/secure-data")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(String.class);
            model.addAttribute("backendResponse", response);
        } catch (Exception e) {
            model.addAttribute("backendResponse", "Error calling backend: " + e.getMessage());
        }
    }
}
