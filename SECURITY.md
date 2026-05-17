# Security Policy

## Scope

OIDC Mock Server is a **development and testing tool** — it is intentionally not hardened for production use. If you are using it in production, that is outside its intended scope and we recommend switching to a production-grade identity provider (Keycloak, Auth0, Okta, etc.).

Known intentional simplifications are documented in [ARCHITECTURE.md](./docs/ARCHITECTURE.md#security-considerations).

## Supported Versions

Only the latest release on the `main` branch receives security fixes.

## Reporting a Vulnerability

If you discover a security issue, please **do not open a public GitHub issue**. Instead:

1. Email **shivamtripathi444@gmail.com** with the subject line `[SECURITY] oidc-mock-server`.
2. Include:
   - A description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Any suggested fix (optional)

You will receive an acknowledgement within **48 hours** and a resolution plan within **7 days**.

## Disclosure Policy

- We follow responsible disclosure: vulnerabilities are fixed before public disclosure.
- We will credit reporters in the release notes unless you prefer to remain anonymous.
- We do not operate a bug bounty program.
