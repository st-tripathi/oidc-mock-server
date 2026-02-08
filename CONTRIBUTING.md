# Contributing to OIDC Mock Server

First off, thank you for considering contributing! 🎉

## How Can I Contribute?

### Reporting Bugs

- Check existing [issues](https://github.com/st-tripathi/oidc-mock-server/issues) first
- Use the bug report template
- Include steps to reproduce

### Suggesting Enhancements

- Open an issue with the `enhancement` label
- Describe the use case and expected behavior

### Pull Requests

1. Fork the repo
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Make your changes
4. Write/update tests
5. Run tests: `./mvnw test`
6. Commit with conventional commits: `feat: add PKCE support`
7. Push and open a PR

## Development Setup

```bash
# Clone your fork
git clone https://github.com/YOUR_USERNAME/oidc-mock-server.git
cd oidc-mock-server

# Build and test
./mvnw clean verify

# Run locally
./mvnw spring-boot:run
```

## Code Style

- Follow existing patterns in the codebase
- Use meaningful variable/method names
- Add Javadoc for public APIs
- Keep methods focused (single responsibility)

## Commit Messages

We use [Conventional Commits](https://www.conventionalcommits.org/):

- `feat:` new feature
- `fix:` bug fix
- `docs:` documentation only
- `refactor:` code refactoring
- `test:` adding tests
- `chore:` maintenance

## Testing

- Write unit tests for services
- Write integration tests for controllers
- Aim for meaningful coverage, not 100%

## Questions?

Open an issue or reach out via GitHub discussions.

---

Thank you for contributing! 🙏
