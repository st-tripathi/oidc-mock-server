package com.oidcmock.controller;

import com.nimbusds.jose.jwk.JWKSet;
import com.oidcmock.config.OidcProperties;
import com.oidcmock.model.OidcConfiguration;
import com.oidcmock.service.TokenService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller for OIDC Discovery endpoints.
 * 
 * <p>
 * Implements:
 * </p>
 * <ul>
 * <li>{@code /.well-known/openid-configuration} - Provider metadata</li>
 * <li>{@code /.well-known/jwks.json} - Public signing keys</li>
 * </ul>
 * 
 * @see <a href=
 *      "https://openid.net/specs/openid-connect-discovery-1_0.html">OIDC
 *      Discovery</a>
 */
@RestController
public class WellKnownController {

    private final OidcProperties properties;
    private final TokenService tokenService;

    public WellKnownController(OidcProperties properties, TokenService tokenService) {
        this.properties = properties;
        this.tokenService = tokenService;
    }

    /**
     * Returns the OIDC discovery document.
     * 
     * <p>
     * Clients use this endpoint to discover server capabilities
     * and endpoint URLs.
     * </p>
     */
    @GetMapping(value = "/.well-known/openid-configuration", produces = MediaType.APPLICATION_JSON_VALUE)
    public OidcConfiguration getConfiguration() {
        return OidcConfiguration.forIssuer(properties.getIssuer());
    }

    /**
     * Returns the JSON Web Key Set (JWKS).
     * 
     * <p>
     * Contains public keys used to verify JWT signatures.
     * Clients should cache this with appropriate TTL.
     * </p>
     */
    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getJwks() {
        JWKSet jwkSet = new JWKSet(tokenService.getPublicKey());
        return jwkSet.toJSONObject();
    }
}
