#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8080}"
USER_ID="${USER_ID:-burst-$(date +%s%N)}"
seq 1 50 | xargs -P50 -I{} curl -sS -X POST "$BASE_URL/wallets" -H "Authorization: Bearer $USER_ID"
printf '\nUSER_ID=%s\n' "$USER_ID"
