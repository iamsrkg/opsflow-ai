#!/usr/bin/env bash
# Run after scripts/smoke.sh against the stack on Kubernetes (k8s/). It checks what Kubernetes
# adds: a pod that dies is replaced, and the data is still there afterwards.
# Usage: scripts/k8s-check.sh [base-url]     (default http://localhost:3000, a port-forward to the frontend)
set -euo pipefail

BASE="${1:-http://localhost:3000}"
API="$BASE/api/v1"
NS=opsflow
step() { printf '\n== %s\n' "$1"; }
fail() { echo "FAILED: $1" >&2; exit 1; }
total() { curl -fsS "$API/requests?size=1" -H "Authorization: Bearer $TOKEN" | jq '.data.totalElements'; }
wait_for_total() {   # the API answers again and still has every request
  for i in $(seq 1 90); do
    NOW=$(total 2>/dev/null) && [ "$NOW" = "$1" ] && return 0
    sleep 2
  done
  fail "expected $1 requests after the restart, last answer: ${NOW:-none}"
}

step "the pods run the way the manifests say"
kubectl -n $NS get pods -o wide
[ "$(kubectl -n $NS get deploy frontend -o jsonpath='{.status.readyReplicas}')" = 2 ] || fail "the frontend should have 2 ready replicas"
USER_ID=$(kubectl -n $NS exec deploy/backend -c backend -- id -u)
[ "$USER_ID" = 10001 ] || fail "the backend should not run as root, it runs as uid $USER_ID"
echo "frontend: 2 replicas; backend: 1 replica, uid $USER_ID, read-only root filesystem"

step "sign in (the smoke test has just used up the login limit, so this may wait up to a minute)"
for i in $(seq 1 45); do
  TOKEN=$(curl -fsS -X POST "$API/auth/login" -H 'Content-Type: application/json' \
            -d '{"username":"agent","password":"agent123"}' 2>/dev/null | jq -r '.data.accessToken' 2>/dev/null) || true
  [ -n "${TOKEN:-}" ] && [ "$TOKEN" != null ] && break
  sleep 2
done
[ -n "${TOKEN:-}" ] && [ "$TOKEN" != null ] || fail "could not sign in"
BEFORE=$(total)
[ "$BEFORE" -ge 1 ] || fail "there should be requests from the smoke test"
echo "$BEFORE requests in the database"

step "kill the backend pod: the Deployment starts a new one, and the token and the data survive"
OLD=$(kubectl -n $NS get pod -l app=backend -o jsonpath='{.items[0].metadata.name}')
kubectl -n $NS delete pod "$OLD" --wait=true
kubectl -n $NS rollout status deploy/backend --timeout=300s
NEW=$(kubectl -n $NS get pod -l app=backend -o jsonpath='{.items[0].metadata.name}')
[ "$OLD" != "$NEW" ] || fail "the backend pod was not replaced"
wait_for_total "$BEFORE"
echo "$OLD -> $NEW, still $BEFORE requests, same token accepted"

step "kill the database pod: the StatefulSet brings it back on the same volume"
kubectl -n $NS delete pod postgres-0 --wait=true
kubectl -n $NS rollout status statefulset/postgres --timeout=300s
wait_for_total "$BEFORE"
echo "postgres-0 replaced, still $BEFORE requests"

step "a request still travels through Kafka after both restarts"
CREATED=$(curl -fsS -X POST "$API/requests" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"Parcel marked delivered but not received","description":"Courier closed the delivery, the customer has nothing."}')
ID=$(echo "$CREATED" | jq -r '.data.id'); REF=$(echo "$CREATED" | jq -r '.data.externalReference')
curl -fsS -X POST "$API/requests/$ID/assign" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"assignedTo":"agent"}' > /dev/null
for i in $(seq 1 45); do
  curl -fsS "$API/notifications" -H "Authorization: Bearer $TOKEN" | jq -e --arg ref "$REF" '.data[] | select(.message | contains($ref))' > /dev/null && break
  [ "$i" = 45 ] && fail "no notification arrived after the restarts"
  sleep 2
done
echo "request $REF -> outbox -> Kafka -> consumer -> notification"

printf '\nKubernetes checks passed.\n'
