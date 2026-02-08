.PHONY: keys up down clean

keys:
	@echo "Generating OIDC keys using container..."
	docker run --rm -v $$(pwd):/app -w /app python:3-slim bash -c "pip install cryptography > /dev/null && python3 scripts/generate-keys.py"

up:
	@if [ ! -f config/jwk.json ]; then $(MAKE) keys; fi
	docker-compose up --build -d

down:
	docker-compose down

clean:
	docker-compose down -v
	rm -f config/jwk.json config/public_key.pem
