#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8080}"
FROM="${FROM:-1}"; TO="${TO:-2}"; KEY="${KEY:-storm-$(date +%s%N)}"; AMOUNT="${AMOUNT:-100}"
seq 1 30 | xargs -P30 -I{} curl -sS -X POST "$BASE_URL/transfers" -H 'Content-Type: application/json' -H 'Authorization: Bearer user-1' -H "Idempotency-Key: $KEY" -d "{\"from\":$FROM,\"to\":$TO,\"amountPaise\":$AMOUNT}"
printf '\n'
