# MS-01 Identity and Access

Spring Boot service (Java 21, hexagonal architecture) for login against Active Directory over LDAPS, RS256 tokens, JWKS, roles, facility scopes and service tokens.

> Status: scaffold. Only the health endpoint is available.

## Run

From the repository root:

```bash
./gradlew :services:ms01-identity:bootRun
curl http://localhost:8081/actuator/health
```

Expected response: `"status":"UP"`.

## Build and test

```bash
./gradlew :services:ms01-identity:build
```

## Configuration

| Variable | Default | Description |
| --- | --- | --- |
| `SERVER_PORT` | `8081` | HTTP port of the service |

Spring Boot does not read `.env` files by itself: export the variables in the shell or pass them through Docker Compose.

## Package layout

Base package: `ec.edu.uce.meditrack.identity`.

| Package | Responsibility |
| --- | --- |
| `domain` | Business model and rules, plain Java |
| `application` | Use cases |
| `ports.in` | Operations offered by the service |
| `ports.out` | Needs from external systems |
| `adapters.web` | REST endpoints and HTTP security |
| `adapters.ldap` | Active Directory over LDAPS |
| `adapters.jwt` | RS256 tokens and public keys |
| `adapters.persistence` | PostgreSQL storage |

Dependencies point inwards. `HexagonalArchitectureTest` fails the build when the core depends on an adapter or on a framework.

## Security

Only `/actuator/health` and its probes are public. Every other path returns `401` until it is opened explicitly.
