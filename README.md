# OIDC Mock Server

[![CI](https://github.com/st-tripathi/oidc-mock-server/actions/workflows/ci.yml/badge.svg)](https://github.com/st-tripathi/oidc-mock-server/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)

A lightweight, configurable OAuth2/OIDC mock server for local development and testing. No cloud dependencies, no complex setup—just run and authenticate.

## ✨ Why This Exists

When building applications that integrate with identity providers (Keycloak, Auth0, Okta), developers often struggle with:

- 🔧 **Complex local setup** — Running full-blown IDPs locally is resource-heavy
- ☁️ **Cloud dependency** — Testing against production IDPs is slow and risky
- 🎭 **Limited test scenarios** — Hard to simulate edge cases (expired tokens, custom claims)

**OIDC Mock Server** solves this by providing a zero-config, in-memory OIDC provider that implements the core OAuth2/OIDC specification.

## 🚀 Quick Start

### Option 1: Docker

```bash
docker run -p 9090:8080 ghcr.io/st-tripathi/oidc-mock-server:latest
```

> [!NOTE]
> The server runs on port **8080** inside the container. We recommend mapping it to **9090** on the host to avoid conflicts with other local services.

### Option 2: From Source

```bash
git clone https://github.com/st-tripathi/oidc-mock-server.git
cd oidc-mock-server
./mvnw spring-boot:run
```

### Verify It's Running

```bash
# If running via Docker (mapped to host port 9090):
curl http://localhost:9090/.well-known/openid-configuration | jq

# If running from source (default port 8080):
curl http://localhost:8080/.well-known/openid-configuration | jq
```

## 📖 Usage

### Default Test User

Out of the box, you can authenticate with:

| Field | Value |
|-------|-------|
| Username | `testuser` |
| Password | `password123` |

### Get an Access Token

```bash
# Using Resource Owner Password flow (for testing only)
curl -X POST http://localhost:8080/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&username=testuser&password=password123&client_id=test-client"
```

### Validate Token at UserInfo

```bash
curl http://localhost:8080/userinfo \
  -H "Authorization: Bearer <your-access-token>"
```

### Introspect a Token

```bash
curl -X POST http://localhost:8080/introspect \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "token=<your-token>"
```

### Full Authorization Code Flow (with PKCE)

1. **Redirect to authorize:**
   ```
   http://localhost:8080/authorize?
     response_type=code&
     client_id=test-client&
     redirect_uri=http://localhost:3000/callback&
     scope=openid profile email&
     code_challenge=<BASE64URL(SHA256(verifier))>&
     code_challenge_method=S256
   ```

2. **Exchange code for tokens:**
   ```bash
   curl -X POST http://localhost:8080/token \
     -d "grant_type=authorization_code&code=<auth-code>&redirect_uri=http://localhost:3000/callback&client_id=test-client&code_verifier=<verifier>"
   ```

## ⚙️ Configuration

### Custom Users

Mount a `users.yaml` file via Docker volume or edit `application.yaml`:

```yaml
# users.yaml (mount to /app/users.yaml in Docker)
oidc:
  users:
    - username: alice
      password: secret123
      claims:
        sub: "user-alice"
        email: "alice@example.com"
        name: "Alice Smith"
        roles:
          - admin

  clients:
    - client-id: my-app
      client-secret: my-secret
      redirect-uris:
        - http://localhost:3000/api/auth/callback/oidc
        - http://localhost:9090/callback
```

Mount when running Docker:
```bash
docker run -p 9090:8080 \
  -v $(pwd)/users.yaml:/app/users.yaml \
  ghcr.io/st-tripathi/oidc-mock-server:latest
```

### Security: Redirect URI Validation

To prevent **Open Redirect** attacks, the server validates `redirect_uri` against the allowlist defined for each client before using it in any redirect. Requests with unknown `client_id` or unregistered `redirect_uri` are rejected with an error page.

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `OIDC_ISSUER` | `http://localhost:8080` | Issuer URL embedded in tokens |
| `OIDC_ACCESS_TOKEN_EXPIRY` | `3600` | Access token lifetime (seconds) |
| `OIDC_ID_TOKEN_EXPIRY` | `3600` | ID token lifetime (seconds) |
| `OIDC_REFRESH_TOKEN_EXPIRY` | `86400` | Refresh token lifetime (seconds) |
| `OIDC_AUTH_CODE_EXPIRY` | `300` | Authorization code lifetime (seconds) |
| `OIDC_SIGNING_KEY_PATH` | Auto-generated | Path to RSA JWK private key file |

## 🔌 Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/.well-known/openid-configuration` | GET | OIDC Discovery document |
| `/.well-known/jwks.json` | GET | JSON Web Key Set (public keys) |
| `/authorize` | GET/POST | Authorization endpoint (login form) |
| `/token` | POST | Token endpoint |
| `/userinfo` | GET | UserInfo endpoint |
| `/introspect` | POST | Token introspection (RFC 7662) |
| `/actuator/health` | GET | Health check |

### Supported Grant Types

| Grant Type | Description |
|------------|-------------|
| `authorization_code` | Standard code flow, with optional PKCE (RFC 7636) |
| `client_credentials` | Machine-to-machine tokens |
| `refresh_token` | Refresh an access token |
| `password` | Direct credentials (dev/testing only) |

## 🏗️ Architecture

See [ARCHITECTURE.md](./docs/ARCHITECTURE.md) for detailed system design, including:
- Component diagrams
- Token flow sequences
- Security considerations

## 🤝 Contributing

We welcome contributions! See [CONTRIBUTING.md](./CONTRIBUTING.md) for guidelines.

## 🔒 Security

This is a **development/testing tool** — not designed for production use. See [SECURITY.md](./SECURITY.md) for the vulnerability disclosure policy and a full list of intentional simplifications.

## 📄 License

This project is licensed under the MIT License - see [LICENSE](./LICENSE) for details.

## 🙏 Acknowledgments

- Inspired by the need for simpler local identity testing
- Built with [Spring Boot](https://spring.io/projects/spring-boot) and [Nimbus JOSE+JWT](https://connect2id.com/products/nimbus-jose-jwt)

---

**Made with ❤️ by [Shivam Tripathi](https://github.com/st-tripathi)**
