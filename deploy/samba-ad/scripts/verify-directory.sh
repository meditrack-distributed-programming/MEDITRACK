#!/usr/bin/env bash
# Checks the directory behaviour that MS-01 relies on. Passwords are never printed.
set -euo pipefail
source /opt/meditrack/lib.sh

readonly PASSWORDS_FILE="$SECRETS_DIR/synthetic-users.csv"
readonly BASE_DN="OU=MediTrack,DC=${DNS_DOMAIN//./,DC=}"
failures=0

password_of() {
    awk -F, -v user="$1" '$1 == user { print $2 }' "$PASSWORDS_FILE"
}

# check <expected: accepted|rejected> <description> <command...>
check() {
    local expected="$1" description="$2" actual=rejected
    shift 2
    if "$@" > /dev/null 2>&1; then
        actual=accepted
    fi
    if [[ "$actual" == "$expected" ]]; then
        echo "PASS  $description: $actual"
    else
        echo "FAIL  $description: $actual, expected $expected"
        failures=$((failures + 1))
    fi
}

# count <expected> <description> <LDAP filter>
count() {
    local expected="$1" description="$2" actual
    actual="$(ldaps ldapsearch -LLL -D "physician01@$DNS_DOMAIN" -w "$(password_of physician01)" \
        -b "$BASE_DN" "$3" dn | grep -c '^dn:' || true)"
    if [[ "$actual" == "$expected" ]]; then
        echo "PASS  $description: $actual"
    else
        echo "FAIL  $description: $actual, expected $expected"
        failures=$((failures + 1))
    fi
}

check accepted "Valid user over LDAPS" \
    ldaps ldapwhoami -D "physician01@$DNS_DOMAIN" -w "$(password_of physician01)"
check rejected "Wrong password" \
    ldaps ldapwhoami -D "physician01@$DNS_DOMAIN" -w "wrong-password"
check rejected "Disabled account" \
    ldaps ldapwhoami -D "physician04@$DNS_DOMAIN" -w "$(password_of physician04)"
check rejected "Password sent without TLS" \
    ldapwhoami -x -o nettimeout=3 -H ldap://localhost:389 \
    -D "physician01@$DNS_DOMAIN" -w "$(password_of physician01)"

count 6 "Role organizational units" "(&(objectClass=organizationalUnit)(!(ou=MediTrack)))"
count 24 "Synthetic users" "(objectClass=user)"
count 3 "Disabled users" \
    "(&(objectClass=user)(userAccountControl:1.2.840.113556.1.4.803:=2))"

if ((failures > 0)); then
    echo "$failures check(s) failed"
    exit 1
fi
echo "All directory checks passed"
