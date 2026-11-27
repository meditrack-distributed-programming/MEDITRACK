# MEDITRACK

MediTrack is a distributed platform for the administrative tracking of medical referrals (traumatology). It uses synthetic data only and is developed for the Distributed Programming course at Universidad Central del Ecuador.

> Status: repository structure only. No application code has been implemented yet.

## Sprint 2 scope

- **MS-01 Identity and Access:** login against Active Directory over LDAPS, RS256 tokens, JWKS and service tokens.
- **MS-06 Notifications:** notification inbox, idempotent escalations and reliable delivery.
- **n8n escalation flow:** MQTT alert to a single notice in MS-06.
- **Login clients:** web, mobile and desktop.

## Technology

Java 21, Spring Boot and Gradle for backend services; React for clients; n8n for automation; Docker Compose for local environments; Terraform for AWS infrastructure.

## Repository layout

```text
.
├── services/
│   ├── ms01-identity/              Identity and Access service
│   └── ms06-notifications/         Notifications service
├── clients/
│   ├── web/                        React web client
│   ├── mobile/                     Capacitor mobile client
│   └── desktop/                    Electron desktop client
├── automation/n8n/                 Exported n8n workflows
├── tools/
│   ├── deadline-alert-simulator/   Synthetic deadline alerts
│   └── lifecycle-event-producer/   Synthetic lifecycle events
├── contracts/
│   ├── proto/                      Protobuf definitions
│   ├── events/                     Event schemas
│   ├── openapi/                    REST API specifications
│   └── mqtt/                       MQTT message schemas
├── infra/terraform/
│   ├── modules/                    Reusable modules
│   └── envs/                       qa (us-east-1) and prod (us-west-2)
├── deploy/
│   ├── compose/                    Docker Compose files
│   ├── k8s/                        Kubernetes manifests
│   ├── nginx/                      Reverse proxy configuration
│   ├── rabbitmq/                   Broker definitions
│   └── samba-ad/                   Directory service setup
├── docs/                           Project documentation
└── .github/                        Workflows, code owners, PR template
```

## Branching rules

| Branch | Purpose |
| --- | --- |
| `develop` | Integration of finished work for the current sprint |
| `qa` | Testing; receives pull requests from `develop` |
| `main` | Production; receives approved pull requests from `qa` |

Never push directly to `develop`, `qa` or `main`.

Short-lived branches use this format:

```text
<type>/MED-<issue-number>-<scope>-<short-description>
```

- Types: `feature`, `fix`, `refactor`, `test`, `docs`, `infra`, `ci`, `chore`, `spike`, `hotfix`. Release branches use `release/vX.Y.Z`.
- Use the Jira sub-task key; use the story key only when the branch covers the whole story.
- Lowercase ASCII, words separated by hyphens, at most 60 characters.
- Branch from `develop` (from `main` only for `hotfix/`) and delete the branch after merge.
- Example: `feature/MED-44-ms01-login-endpoint`

Scopes: `ms01` to `ms12`, `web`, `mobile`, `desktop`, `n8n`, `tools`, `contracts`, `infra`, `deploy`, `ci`, `docs`, `repo`.

## Commit rules

Commits follow [Conventional Commits 1.0.0](https://www.conventionalcommits.org/en/v1.0.0/):

```text
feat(ms01): add ldaps login endpoint with rs256 tokens

Refs: MED-44
```

- Types: `feat`, `fix`, `perf`, `refactor`, `test`, `docs`, `build`, `ci`, `style`, `chore`, `revert`. Infrastructure changes use `build(infra)`.
- The scope is mandatory.
- Subject in imperative mood, lowercase, no final period; header of at most 72 characters.
- Every commit has a `Refs: MED-<n>` footer.
- One logical change per commit.
- Never commit `.env` files, Terraform state, keys, certificates or n8n credentials.

## Pull requests

- Feature work targets `develop` and is squash merged.
- The title is a Conventional Commit header with the Jira key, for example `feat(ms01): add ldaps login endpoint [MED-44]`.
- Required checks: build and tests of the affected service, commitlint, branch name and pull request title.
