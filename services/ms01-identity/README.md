# MS-01 Identity and Access

Spring Boot service (Java 21, hexagonal architecture) for login against Active Directory over LDAPS, RS256 tokens, JWKS, roles, facility scopes and service tokens.

> Status: the directory adapter is implemented. There is no login endpoint yet; only the health endpoint is available.

## Run

From the repository root:

```bash
./gradlew :services:ms01-identity:bootRun
curl http://localhost:8081/actuator/health
```

Expected response: `"status":"UP"`. The directory is contacted only when somebody signs in, so the service also starts without it.

## Connect to the local directory

Start the Samba controller and export the certificate of its authority. In Git Bash, from the repository root:

```bash
docker compose -f deploy/compose/docker-compose.yml up -d --wait
MSYS_NO_PATHCONV=1 docker compose -f deploy/compose/docker-compose.yml cp \
  samba-ad:/var/lib/samba/private/tls/ca.crt ./meditrack-local-ca.crt
LDAP_CA_CERTIFICATE="$(pwd -W)/meditrack-local-ca.crt" ./gradlew :services:ms01-identity:bootRun
```

## Build and test

```bash
./gradlew :services:ms01-identity:build
```

`SambaDirectoryIntegrationTest` builds a temporary domain controller from `deploy/samba-ad`, which takes a few minutes. It needs Docker and is skipped when Docker is not running.

## Configuration

| Variable | Default | Description |
| --- | --- | --- |
| `SERVER_PORT` | `8081` | HTTP port of the service |
| `LDAP_URL` | `ldaps://localhost:636` | Domain controller; only `ldaps` is accepted |
| `LDAP_DOMAIN` | `meditrack.local` | Domain appended to the user name |
| `LDAP_BASE_DN` | `OU=MediTrack,DC=meditrack,DC=local` | Branch that holds the MediTrack accounts |
| `LDAP_CA_CERTIFICATE` | empty | PEM file of the directory certificate authority; when empty only public authorities are trusted |

Spring Boot does not read `.env` files by itself: export the variables in the shell or pass them through Docker Compose.

## Directory authentication

`DirectoryPort` is implemented by `adapters.ldap`:

1. The user name and password are checked first. An empty password never reaches the directory.
2. The adapter signs in over LDAPS as `user@domain`, trusting the configured certificate authority and verifying the host name.
3. With the same connection it reads `sAMAccountName`, `objectGUID` and `userAccountControl` of that account.

| Situation | Result |
| --- | --- |
| Valid and enabled account inside the base branch | The account and its stable identifier |
| Wrong password, unknown user, disabled or locked account, account outside the base branch | `InvalidCredentialsException`, the same for all of them |
| Directory unreachable, too slow or not trusted | `DirectoryUnavailableException` |

Time limits: 2 seconds to connect and 3 seconds to wait for an answer. Passwords are never stored or logged. Retries and the circuit breaker are added by their own tasks.

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
