# Samba AD

Active Directory compatible domain controller used to develop MS-01 on a laptop. It is **not** used in AWS: the QA environment uses Active Directory on Windows Server (MED-57 to MED-59).

## What it creates

- Domain `meditrack.local` (NetBIOS name `MEDITRACK`).
- LDAPS on port 636, with a certificate signed by a local certificate authority created on the first start. Passwords sent without TLS are rejected.
- Organizational unit `OU=MediTrack` with one unit per application role.
- 24 synthetic users with invented names, three of them disabled for negative tests. The names are listed in `scripts/users.csv`; none of them represents a real person.

| Organizational unit | Users | Disabled |
| --- | --- | --- |
| `OriginatingPhysicians` | `physician01` to `physician04` | `physician04` |
| `OriginatingCoordinators` | `origcoord01` to `origcoord04` | none |
| `ReceivingCoordinators` | `recvcoord01` to `recvcoord04` | `recvcoord04` |
| `Patients` | `patient01` to `patient04` | `patient04` |
| `Supervisors` | `supervisor01` to `supervisor04` | none |
| `Administrators` | `mtadmin01` to `mtadmin04` | none |

The directory only authenticates. Application roles and facility scopes are stored by MS-01.

## Run

From the repository root:

```bash
docker compose -f deploy/compose/docker-compose.yml up -d --build --wait
docker compose -f deploy/compose/docker-compose.yml exec samba-ad bash /opt/meditrack/verify-directory.sh
```

The second command checks that a valid user is accepted, that a wrong password, a disabled account and a password sent without TLS are rejected, and that the units and users exist.

## Passwords and certificate

Passwords are generated on the first start, stay inside the `samba-data` volume and are never committed. In Git Bash:

```bash
# Passwords of the synthetic users
MSYS_NO_PATHCONV=1 docker compose -f deploy/compose/docker-compose.yml exec samba-ad \
  cat /var/lib/samba/meditrack-secrets/synthetic-users.csv

# Certificate of the local authority, needed by LDAPS clients
MSYS_NO_PATHCONV=1 docker compose -f deploy/compose/docker-compose.yml cp \
  samba-ad:/var/lib/samba/private/tls/ca.crt ./meditrack-local-ca.crt
```

Certificates are ignored by Git. The certificate is valid for `localhost`, `samba-ad`, `dc1` and `dc1.meditrack.local`.

## Local and AWS directory

MS-01 uses the same LDAPS protocol against both directories, so only its configuration changes. The variable names are defined by the LDAPS adapter (MED-42).

| Setting | Local (Samba) | AWS QA (Windows Server) |
| --- | --- | --- |
| Address | `ldaps://localhost:636`, or `ldaps://samba-ad:636` inside Compose | `ldaps://` private name of the domain controller |
| Domain | `meditrack.local` | `meditrack.local` |
| Base DN | `OU=MediTrack,DC=meditrack,DC=local` | `OU=MediTrack,DC=meditrack,DC=local` |
| Trusted certificate | Local certificate authority of the container | Certificate exported from the server (MED-58) |
