# OIDC Mock Server

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

### Full Authorization Code Flow

1. **Redirect to authorize:**
   ```
   http://localhost:8080/authorize?
     response_type=code&
     client_id=test-client&
     redirect_uri=http://localhost:3000/callback&
     scope=openid profile email
   ```

2. **Exchange code for tokens:**
   ```bash
   curl -X POST http://localhost:8080/token \
     -d "grant_type=authorization_code&code=<auth-code>&redirect_uri=http://localhost:3000/callback&client_id=test-client"
   ```

## ⚙️ Configuration

### Custom Users

Create a `users.yaml` file or set environment variables:

```yaml
# application.yaml or mounted config
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
      redirect-uris:
        - http://localhost:3000/api/auth/callback/oidc
        - http://localhost:9090/callback
```

### Security: Redirect URI Validation

To prevent **Open Redirect** attacks, the server strictly validates `redirect_uri` against the whitelisted `redirect-uris` defined for each client. If a request is made with an unknown `client_id` or an unregistered `redirect_uri`, the server will display an error and refuse to proceed.


Mount when running Docker:
```bash
docker run -p 8080:8080 -v $(pwd)/users.yaml:/app/users.yaml ghcr.io/st-tripathi/oidc-mock-server:latest
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `OIDC_ISSUER` | `http://localhost:8080` | Issuer URL in tokens |
| `OIDC_TOKEN_EXPIRY` | `3600` | Token lifetime in seconds |
| `OIDC_SIGNING_KEY` | Auto-generated | RSA private key for JWT signing |

## 🔌 Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/.well-known/openid-configuration` | GET | OIDC Discovery document |
| `/.well-known/jwks.json` | GET | JSON Web Key Set |
| `/authorize` | GET | Authorization endpoint |
| `/token` | POST | Token endpoint |
| `/userinfo` | GET | UserInfo endpoint |

## 🏗️ Architecture

See [ARCHITECTURE.md](./docs/ARCHITECTURE.md) for detailed system design, including:
- Component diagrams
- Token flow sequences
- Security considerations

## 🤝 Contributing

We welcome contributions! See [CONTRIBUTING.md](./CONTRIBUTING.md) for guidelines.

## 📄 License

This project is licensed under the MIT License - see [LICENSE](./LICENSE) for details.

## 🙏 Acknowledgments

- Inspired by the need for simpler local identity testing
- Built with [Spring Boot](https://spring.io/projects/spring-boot) and [Nimbus JOSE+JWT](https://connect2id.com/products/nimbus-jose-jwt)

---

**Made with ❤️ by [Shivam Tripathi](https://github.com/st-tripathi)**
