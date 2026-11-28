#!/usr/bin/env bash
# Creates one organizational unit per application role and the synthetic users of users.csv.
# Passwords are generated here and stay inside the container volume.
set -euo pipefail
source /opt/meditrack/lib.sh

readonly SEEDED_MARKER="$STATE_DIR/.seeded"
readonly ROOT_OU="OU=MediTrack"
readonly PASSWORDS_FILE="$SECRETS_DIR/synthetic-users.csv"

[[ -f "$SEEDED_MARKER" ]] && exit 0

# Safe to repeat: an interrupted first start can run this script again
ensure_ou() {
    if ! samba-tool ou list | grep -qxF "$1"; then
        samba-tool ou add "$1"
    fi
}

ensure_ou "$ROOT_OU"
install -d -m 700 "$SECRETS_DIR"
(umask 077 && echo "username,password,enabled" > "$PASSWORDS_FILE")

# The list is read from descriptor 3 so that samba-tool cannot consume it
while IFS=, read -r -u 3 username ou given_name surname enabled; do
    ensure_ou "OU=$ou,$ROOT_OU"
    password="$(random_password)"

    if samba-tool user show "$username" > /dev/null 2>&1; then
        samba-tool user setpassword "$username" --newpassword="$password"
    else
        samba-tool user create "$username" "$password" \
            --userou="OU=$ou,$ROOT_OU" \
            --given-name="$given_name" \
            --surname="$surname"
    fi

    if [[ "$enabled" == "false" ]]; then
        samba-tool user disable "$username"
    fi

    echo "$username,$password,$enabled" >> "$PASSWORDS_FILE"
done 3< <(tail -n +2 "$SCRIPTS_DIR/users.csv")

touch "$SEEDED_MARKER"
echo "Directory seeded with $(($(wc -l < "$PASSWORDS_FILE") - 1)) synthetic users"
