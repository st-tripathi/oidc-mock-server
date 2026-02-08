# OIDC Integration Demo

This demo showcases a full OIDC flow involving:
- **IdP**: OIDC Mock Server
- **Frontend**: Spring Boot Web App (OAuth2 Client)
- **API Gateway**: Kong Gateway (JWT Verification)
- **Backend**: Echo Service (Protected Resource)

## 🚀 Quick Start

1. **Generate Keys & Start**:
   ```bash
   make up
   ```

2. **Access the App**: [http://localhost:8082](http://localhost:8082)
3. **Login**: Use `demo-user` / `password`.
4. **Observe**: The app will display your OIDC claims and the response from the microservice (fetched via Kong).

## 🔑 Key Management

We use a static RSA keypair for this demo so that Kong can verify tokens issued by the IdP.

To regenerate keys:
```bash
make keys
```
*Complexity Note: We use a python container to generate these because OpenSSL CLI does not natively output the JWK JSON format required by OIDC servers.*

## 🏗️ Architecture

- **Frontend** (8082): Performs OIDC login -> receives Access Token -> sends token to Kong.
- **Kong** (8000): Validates JWT signature -> Proxies to Backend.
- **Backend**: Simple echo service.
