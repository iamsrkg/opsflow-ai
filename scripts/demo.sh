#!/usr/bin/env bash
set -euo pipefail

BASE_URL="http://localhost:8080/api/v1"

echo "=== OpsFlow AI Automated API Demo ==="

echo "1. Logging in as operator..."
TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"operator","password":"operator123"}' | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

echo "Acquired Token: ${TOKEN:0:15}..."

echo "2. Creating Operational Request..."
REQ_ID=$(curl -s -X POST "$BASE_URL/requests" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Charged ₹500 but order payment pending","description":"Money deducted at 10:31 AM, Txn #TXN-99482104, order status pending.","category":"PAYMENT","priority":"HIGH"}' | grep -o '"id":[^,]*' | head -n 1 | cut -d':' -f2)

echo "Created Request ID: $REQ_ID"

echo "3. Triggering AI Investigation..."
curl -s -X POST "$BASE_URL/requests/$REQ_ID/investigate" \
  -H "Authorization: Bearer $TOKEN"

echo "4. Fetching Pending Approvals..."
curl -s -X GET "$BASE_URL/approvals/pending" \
  -H "Authorization: Bearer $TOKEN"

echo "=== Demo Execution Complete ==="
