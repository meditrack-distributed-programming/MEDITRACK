#!/usr/bin/env bash
# Provisions the domain on the first start and then runs the domain controller.
set -euo pipefail
source /opt/meditrack/lib.sh

readonly PROVISIONED_MARKER="$STATE_DIR/.provisioned"

# Certificate authority and server certificate used by LDAPS, created inside the container
create_certificates() {
    install -d -m 700 "$TLS_DIR"
    openssl req -x509 -newkey rsa:3072 -nodes -days 365 \
        -subj "/CN=MediTrack Local CA" \
        -keyout "$TLS_DIR/ca.key" -out "$CA_CERTIFICATE" 2>/dev/null
    openssl req -newkey rsa:3072 -nodes \
        -subj "/CN=dc1.$DNS_DOMAIN" \
        -keyout "$TLS_DIR/server.key" -out "$TLS_DIR/server.csr" 2>/dev/null
    openssl x509 -req -days 365 -in "$TLS_DIR/server.csr" \
        -CA "$CA_CERTIFICATE" -CAkey "$TLS_DIR/ca.key" -CAcreateserial \
        -extfile <(printf '%s\n' \
            "subjectAltName=DNS:dc1.$DNS_DOMAIN,DNS:dc1,DNS:samba-ad,DNS:localhost,IP:127.0.0.1" \
            "extendedKeyUsage=serverAuth") \
        -out "$TLS_DIR/server.crt" 2>/dev/null
    rm "$TLS_DIR/server.csr"
    chmod 600 "$TLS_DIR/ca.key" "$TLS_DIR/server.key"
}

provision_domain() {
    local admin_password="${SAMBA_ADMIN_PASSWORD:-$(random_password)}"

    # Extended attributes are kept in a database file, so no privileged container is needed
    local vfs_objects="dfs_samba4 acl_xattr xattr_tdb"

    rm -f /etc/samba/smb.conf
    samba-tool domain provision \
        --realm="$SAMBA_REALM" \
        --domain="$SAMBA_DOMAIN" \
        --server-role=dc \
        --dns-backend=SAMBA_INTERNAL \
        --adminpass="$admin_password" \
        --option="vfs objects = $vfs_objects"

    # The dynamic DNS update is skipped because clients reach the controller through Docker names
    sed -i "/^\[global\]/a\\
\\tdns update command = /bin/true\\
\\tldap server require strong auth = yes\\
\\ttls enabled = yes\\
\\ttls keyfile = $TLS_DIR/server.key\\
\\ttls certfile = $TLS_DIR/server.crt\\
\\ttls cafile = $CA_CERTIFICATE" /etc/samba/smb.conf

    install -d -m 700 "$SECRETS_DIR"
    (umask 077 && printf '%s\n' "$admin_password" > "$SECRETS_DIR/administrator-password")
}

if [[ ! -f "$PROVISIONED_MARKER" ]]; then
    echo "Provisioning the $SAMBA_REALM domain"
    provision_domain
    create_certificates
    touch "$PROVISIONED_MARKER"
fi

# Creates the organizational units and users when the seed script is part of the image
if [[ -f "$SCRIPTS_DIR/seed-directory.sh" ]]; then
    bash "$SCRIPTS_DIR/seed-directory.sh"
fi

exec samba --foreground --no-process-group --debug-stdout
