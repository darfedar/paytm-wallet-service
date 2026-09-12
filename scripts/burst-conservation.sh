#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

echo "1. Creating two fresh wallets..."
USER_A="user-a-$(date +%s%N)"
USER_B="user-b-$(date +%s%N)"

WALLET_A=$(curl -sS -X POST "$BASE_URL/wallets" -H "Authorization: Bearer $USER_A" | grep -o '"walletId":[0-9]*' | cut -d: -f2)
WALLET_B=$(curl -sS -X POST "$BASE_URL/wallets" -H "Authorization: Bearer $USER_B" | grep -o '"walletId":[0-9]*' | cut -d: -f2)

echo "Wallet A ID: $WALLET_A"
echo "Wallet B ID: $WALLET_B"

echo "2. Funding wallets with 10,000 paise each..."
curl -sS -X POST "$BASE_URL/test/wallets/$WALLET_A/fund?amountPaise=10000" > /dev/null
curl -sS -X POST "$BASE_URL/test/wallets/$WALLET_B/fund?amountPaise=10000" > /dev/null

echo "3. Firing concurrent transfers (A->B, B->A, and overdraft attempts)..."
# 30 valid transfers A->B
for i in {1..30}; do
  curl -sS -X POST "$BASE_URL/transfers" -H "Authorization: Bearer $USER_A" -H "Content-Type: application/json" -H "Idempotency-Key: a2b-$USER_A-$i" -d "{\"from\":$WALLET_A,\"to\":$WALLET_B,\"amountPaise\":1000}" > /dev/null &
done

# 30 valid transfers B->A
for i in {1..30}; do
  curl -sS -X POST "$BASE_URL/transfers" -H "Authorization: Bearer $USER_B" -H "Content-Type: application/json" -H "Idempotency-Key: b2a-$USER_B-$i" -d "{\"from\":$WALLET_B,\"to\":$WALLET_A,\"amountPaise\":1000}" > /dev/null &
done

# 20 overdraft attempts A->B (asking for 100,000 paise)
for i in {1..20}; do
  curl -sS -X POST "$BASE_URL/transfers" -H "Authorization: Bearer $USER_A" -H "Content-Type: application/json" -H "Idempotency-Key: over-$USER_A-$i" -d "{\"from\":$WALLET_A,\"to\":$WALLET_B,\"amountPaise\":100000}" > /dev/null &
done

# Wait for all background curl processes to finish
wait

echo "4. Checking final balances..."
BAL_A=$(curl -sS -X GET "$BASE_URL/wallets/$WALLET_A" | grep -o '"balancePaise":[0-9]*' | cut -d: -f2)
BAL_B=$(curl -sS -X GET "$BASE_URL/wallets/$WALLET_B" | grep -o '"balancePaise":[0-9]*' | cut -d: -f2)

echo "Wallet A Final Balance: $BAL_A"
echo "Wallet B Final Balance: $BAL_B"

TOTAL=$((BAL_A + BAL_B))
echo "Sum of both wallets: $TOTAL paise (Expected: 20000)"

if [ "$TOTAL" -eq 20000 ]; then
  echo "✅ CONSERVATION PASSED: No money was lost or created out of thin air!"
else
  echo "❌ CONSERVATION FAILED: Money was leaked!"
  exit 1
fi
