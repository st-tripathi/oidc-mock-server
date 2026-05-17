package com.oidcmock.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * OIDC Discovery document as per OpenID Connect Discovery 1.0.
 *
 * @see <a href="https://openid.net/specs/openid-connect-discovery-1_0.html">OIDC Discovery</a>
 */
public record OidcConfiguration(
    @JsonProperty("issuer")
    String issuer,

    @JsonProperty("authorization_endpoint")
    String authorizationEndpoint,

    @JsonProperty("token_endpoint")
    String tokenEndpoint,

    @JsonProperty("userinfo_endpoint")
    String userinfoEndpoint,

    @JsonProperty("jwks_uri")
    String jwksUri,

    @JsonProperty("introspection_endpoint")
    String introspectionEndpoint,

    @JsonProperty("response_types_supported")
    List<String> responseTypesSupported,

    @JsonProperty("subject_types_supported")
    List<String> subjectTypesSupported,

    @JsonProperty("id_token_signing_alg_values_supported")
    List<String> idTokenSigningAlgValuesSupported,

    @JsonProperty("scopes_supported")
    List<String> scopesSupported,

    @JsonProperty("token_endpoint_auth_methods_supported")
    List<String> tokenEndpointAuthMethodsSupported,

    @JsonProperty("claims_supported")
    List<String> claimsSupported,

    @JsonProperty("grant_types_supported")
    List<String> grantTypesSupported,

    @JsonProperty("code_challenge_methods_supported")
    List<String> codeChallengeMethodsSupported
) {
    public static OidcConfiguration forIssuer(String issuer) {
        return new OidcConfiguration(
            issuer,
            issuer + "/authorize",
            issuer + "/token",
            issuer + "/userinfo",
            issuer + "/.well-known/jwks.json",
            issuer + "/introspect",
            List.of("code", "token"),
            List.of("public"),
            List.of("RS256"),
            List.of("openid", "profile", "email", "roles"),
            List.of("client_secret_basic", "client_secret_post"),
            List.of("sub", "name", "email", "email_verified", "given_name", "family_name", "roles"),
            List.of("authorization_code", "client_credentials", "refresh_token", "password"),
            List.of("S256", "plain")
        );
    }
}
