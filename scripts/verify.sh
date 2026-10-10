#!/usr/bin/env bash
set -euo pipefail

echo "=== Verifying OpsFlow AI Local Build ==="

echo "1. Testing Backend Maven Build..."
(cd backend && ./mvnw test)

echo "2. Testing Frontend Production Build..."
(cd frontend && npm install && npm run build)

echo "=== All Verification Checks Passed Cleanly! ==="
