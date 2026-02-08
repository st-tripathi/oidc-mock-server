# Architecture

This document describes the system design of OIDC Mock Server.

## Overview

OIDC Mock Server is a lightweight implementation of the OpenID Connect Core specification, providing just enough functionality to support local development and testing of OAuth2/OIDC-integrated applications.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           OIDC Mock Server                                  │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                        API Layer (Controllers)                       │   │
│  │                                                                       │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌─────────┐  ┌──────────────┐   │   │
│  │  │ WellKnown    │  │ Authorization│  │ Token   │  │ UserInfo     │   │   │
│  │  │ Controller   │  │ Controller   │  │Controller│  │ Controller   │   │   │
│  │  └──────────────┘  └──────────────┘  └─────────┘  └──────────────┘   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                        Domain Layer (Services)                       │   │
│  │                                                                       │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐               │   │
│  │  │ TokenService │  │ UserService  │  │ AuthCode     │               │   │
│  │  │              │  │              │  │ Service      │               │   │
│  │  │ - Sign JWT   │  │ - Lookup     │  │ - Generate   │               │   │
│  │  │ - Verify JWT │  │ - Validate   │  │ - Exchange   │               │   │
│  │  │ - Build claims│ │ - Get claims │  │ - Expire     │               │   │
│  │  └──────────────┘  └──────────────┘  └──────────────┘               │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                     Configuration Layer                              │   │
│  │                                                                       │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐               │   │
│  │  │ users.yaml   │  │ application  │  │ RSA KeyPair  │               │   │
│  │  │              │  │ .yaml        │  │ (in-memory)  │               │   │
│  │  └──────────────┘  └──────────────┘  └──────────────┘               │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Component Details

### 1. WellKnownController

Implements OIDC Discovery ([RFC 8414](https://tools.ietf.org/html/rfc8414)):

- `GET /.well-known/openid-configuration` — Returns provider metadata
- `GET /.well-known/jwks.json` — Returns public signing keys

### 2. AuthorizationController

Implements Authorization Endpoint ([RFC 6749 §3.1](https://tools.ietf.org/html/rfc6749#section-3.1)):

- Validates `client_id`, `redirect_uri`, `scope`
- Renders simple login form
- Issues authorization codes
- Redirects back to client with code

### 3. TokenController

Implements Token Endpoint ([RFC 6749 §3.2](https://tools.ietf.org/html/rfc6749#section-3.2)):

- Exchanges authorization code for tokens
- Supports `grant_type`:
  - `authorization_code`
  - `password` (for testing convenience)
- Returns `access_token`, `id_token`, `refresh_token`

### 4. UserInfoController

Implements UserInfo Endpoint ([OIDC Core §5.3](https://openid.net/specs/openid-connect-core-1_0.html#UserInfo)):

- Validates Bearer token
- Returns user claims as JSON

---

## Token Flow Sequence

### Authorization Code Flow

```
┌──────────┐       ┌─────────────────┐       ┌──────────────────┐
│  Client  │       │   Mock Server   │       │   User Browser   │
└────┬─────┘       └────────┬────────┘       └────────┬─────────┘
     │                      │                         │
     │ 1. Redirect to /authorize                      │
     │─────────────────────────────────────────────────>
     │                      │                         │
     │                      │   2. Show login form    │
     │                      │<─────────────────────────
     │                      │                         │
     │                      │   3. Submit credentials │
     │                      │<─────────────────────────
     │                      │                         │
     │   4. Redirect with auth code                   │
     │<─────────────────────────────────────────────────
     │                      │                         │
     │ 5. POST /token       │                         │
     │      (code exchange) │                         │
     │─────────────────────>│                         │
     │                      │                         │
     │ 6. Return tokens     │                         │
     │<─────────────────────│                         │
     │                      │                         │
     │ 7. GET /userinfo     │                         │
     │   (with Bearer token)│                         │
     │─────────────────────>│                         │
     │                      │                         │
     │ 8. Return user claims│                         │
     │<─────────────────────│                         │
```

---

## Security Considerations

### What This Is

- A **mock server for development/testing**
- Designed to be simple, not secure
- Uses in-memory state (no persistence)

### What This Is NOT

- A production identity provider
- A security-hardened service
- A replacement for Keycloak/Auth0/Okta

### Intentional Simplifications

| Production IDP | Mock Server |
|----------------|-------------|
| Hashed passwords (bcrypt) | Plaintext comparison |
| CSRF protection | None |
| Rate limiting | None |
| Session management | Stateless |
| Audit logging | Basic |
| PKCE enforcement | Optional |

---

## Design Decisions

### ADR-001: In-Memory State

**Context:** Need to store authorization codes temporarily.

**Decision:** Use `ConcurrentHashMap` with TTL-based expiry.

**Rationale:** 
- No external dependencies
- Simple to test
- Sufficient for mock server use case

**Trade-off:** State lost on restart (acceptable for dev/test).

---

### ADR-002: RSA Key Generation

**Context:** Need signing keys for JWTs.

**Decision:** Generate RSA keypair on startup.

**Rationale:**
- Zero configuration
- Matches production behavior (asymmetric signing)
- JWKS endpoint allows token verification

**Trade-off:** Key changes on restart (tokens invalidated).

---

### ADR-003: YAML-Based User Configuration

**Context:** Need configurable test users.

**Decision:** Load users from `users.yaml` or Spring configuration.

**Rationale:**
- Declarative, version-controllable
- Easy to understand and modify
- Maps directly to Spring's `@ConfigurationProperties`

---

## Technology Stack

| Layer | Technology | Justification |
|-------|------------|---------------|
| Framework | Spring Boot 3.2 | Industry standard, excellent testing support |
| JWT | Nimbus JOSE+JWT | Reference implementation, well-maintained |
| Build | Maven | Familiar, reliable |
| Runtime | Java 21 | LTS, virtual threads ready |
