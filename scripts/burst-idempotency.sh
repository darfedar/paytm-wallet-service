#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8080}"

echo "1. Creating fresh wallets for idempotency test..."
USER_A="idem-a-$(date +%s%N)"
USER_B="idem-b-$(date +%s%N)"

WALLET_A=$(curl -sS -X POST "$BASE_URL/wallets" -H "Authorization: Bearer $USER_A" | grep -o '"walletId":[0-9]*' | cut -d: -f2)
WALLET_B=$(curl -sS -X POST "$BASE_URL/wallets" -H "Authorization: Bearer $USER_B" | grep -o '"walletId":[0-9]*' | cut -d: -f2)

echo "Wallet A: $WALLET_A, Wallet B: $WALLET_B"
echo "2. Funding Wallet A..."
curl -sS -X POST "$BASE_URL/test/wallets/$WALLET_A/fund?amountPaise=5000" > /dev/null

KEY="storm-$(date +%s%N)"
AMOUNT=100

echo "3. Firing 30 concurrent exact-duplicate transfers (A->B)..."
seq 1 30 | xargs -P30 -I{} curl -sS -X POST "$BASE_URL/transfers" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $USER_A" \
  -H "Idempotency-Key: $KEY" \
  -d "{\"from\":$WALLET_A,\"to\":$WALLET_B,\"amountPaise\":$AMOUNT}"

printf '\n\nDone! All responses above should have the exact same transferId.\n'
