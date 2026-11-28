# Samba Active Directory domain controller for local development only.
# AWS environments use Active Directory on Windows Server.
FROM debian:trixie-slim

RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        ca-certificates \
        ldap-utils \
        openssl \
        samba-ad-dc \
        samba-ad-provision \
        samba-dsdb-modules \
        samba-vfs-modules \
        winbind \
    && rm -rf /var/lib/apt/lists/*

ENV SAMBA_REALM=MEDITRACK.LOCAL \
    SAMBA_DOMAIN=MEDITRACK

COPY scripts/ /opt/meditrack/

# Only LDAPS is offered to other containers and to the host
EXPOSE 636

HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=6 \
    CMD ["bash", "/opt/meditrack/healthcheck.sh"]

# A domain controller has to run as root, unlike the application containers
ENTRYPOINT ["bash", "/opt/meditrack/entrypoint.sh"]
