# Docker Compose

Compose files for the local environment.

| Service | Purpose | Address on this computer |
| --- | --- | --- |
| `samba-ad` | Active Directory compatible domain controller | `ldaps://localhost:636` |

## Commands

Run them from the repository root.

```bash
# Build and start, and wait until the services are healthy
docker compose -f deploy/compose/docker-compose.yml up -d --build --wait

# State and logs
docker compose -f deploy/compose/docker-compose.yml ps
docker compose -f deploy/compose/docker-compose.yml logs samba-ad

# Stop, keeping the data
docker compose -f deploy/compose/docker-compose.yml down

# Stop and delete the data: the next start creates a new domain and new passwords
docker compose -f deploy/compose/docker-compose.yml down -v
```

No `.env` file is needed. To change a default, copy `.env.example` to `.env`.
