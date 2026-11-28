#!/usr/bin/env bash
# Healthy when the directory answers over LDAPS with a certificate of the local authority.
set -euo pipefail
source /opt/meditrack/lib.sh

ldaps ldapsearch -LLL -b "" -s base defaultNamingContext > /dev/null
