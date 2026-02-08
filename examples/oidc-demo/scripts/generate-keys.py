import json
import base64
import os
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.backends import default_backend

def b64url(n):
    return base64.urlsafe_b64encode(n).decode('utf-8').replace('=', '')

def to_b64url_uint(n):
    b = n.to_bytes((n.bit_length() + 7) // 8, byteorder='big')
    return b64url(b)

# Generate RSA keypair
private_key = rsa.generate_private_key(
    public_exponent=65537,
    key_size=2048,
    backend=default_backend()
)

numbers = private_key.private_numbers()
public_numbers = numbers.public_numbers

# Generate JWK (for IdP)
jwk = {
    "kty": "RSA",
    "kid": "demo-kid",
    "use": "sig",
    "alg": "RS256",
    "n": to_b64url_uint(public_numbers.n),
    "e": to_b64url_uint(public_numbers.e),
    "d": to_b64url_uint(numbers.d),
    "p": to_b64url_uint(numbers.p),
    "q": to_b64url_uint(numbers.q),
    "dp": to_b64url_uint(numbers.dmp1),
    "dq": to_b64url_uint(numbers.dmq1),
    "qi": to_b64url_uint(numbers.iqmp)
}

# Generate Public PEM (for Kong)
pem = public_numbers.public_key(default_backend()).public_bytes(
    encoding=serialization.Encoding.PEM,
    format=serialization.PublicFormat.SubjectPublicKeyInfo
)

# Ensure config directory exists
os.makedirs('config', exist_ok=True)

with open('config/jwk.json', 'w') as f:
    json.dump(jwk, f, indent=2)

with open('config/public_key.pem', 'wb') as f:
    f.write(pem)

print("Successfully generated config/jwk.json and config/public_key.pem")
