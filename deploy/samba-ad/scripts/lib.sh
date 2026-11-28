# Shared settings and helpers of the domain controller scripts. Sourced, not executed.

readonly SCRIPTS_DIR=/opt/meditrack
readonly STATE_DIR=/var/lib/samba
readonly SECRETS_DIR="$STATE_DIR/meditrack-secrets"
readonly TLS_DIR="$STATE_DIR/private/tls"
readonly CA_CERTIFICATE="$TLS_DIR/ca.crt"
readonly DNS_DOMAIN="${SAMBA_REALM,,}"

# Random password that satisfies the default complexity rules of Active Directory
random_password() {
    local raw
    raw="$(openssl rand -base64 48 | tr -dc 'A-Za-z0-9')"
    printf 'Mt1!%s' "${raw:0:16}"
}

# Runs an OpenLDAP client over LDAPS, trusting only the local certificate authority
ldaps() {
    local command="$1"
    shift
    LDAPTLS_CACERT="$CA_CERTIFICATE" LDAPTLS_REQCERT=demand \
        "$command" -x -o nettimeout=3 -H ldaps://localhost:636 "$@"
}
