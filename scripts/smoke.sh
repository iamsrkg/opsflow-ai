#!/usr/bin/env bash
# Exercises the running stack from the outside, through the same door a browser uses.
# Usage: scripts/smoke.sh [base-url]     (default http://localhost:3000)
set -euo pipefail

BASE="${1:-http://localhost:3000}"
API="$BASE/api/v1"
step() { printf '\n== %s\n' "$1"; }
fail() { echo "FAILED: $1" >&2; exit 1; }
login() { curl -fsS -X POST "$API/auth/login" -H 'Content-Type: application/json' \
            -d "{\"username\":\"$1\",\"password\":\"$1123\"}" | jq -r '.data.accessToken'; }

step "wait for the backend (through nginx)"
for i in $(seq 1 90); do
  [ "$(curl -fsS "$BASE/actuator/health" 2>/dev/null | jq -r '.status' 2>/dev/null)" = "UP" ] && break
  [ "$i" = 90 ] && fail "the backend never reported UP"
  sleep 2
done
echo "UP"

step "the dashboard is served"
curl -fsS "$BASE/" | grep -q "OpsFlow" || fail "the dashboard page did not load"

step "sign in"
AGENT=$(login agent); MANAGER=$(login manager)
[ -n "$AGENT" ] && [ "$AGENT" != null ] || fail "agent could not sign in"
echo "ok"

step "create a request, assign it, and wait for the notification the Kafka consumer writes"
CREATED=$(curl -fsS -X POST "$API/requests" -H "Authorization: Bearer $AGENT" -H 'Content-Type: application/json' \
  -d '{"title":"Customer charged but order still pending","description":"Payment was deducted from the card, the order status did not update."}')
ID=$(echo "$CREATED" | jq -r '.data.id'); REF=$(echo "$CREATED" | jq -r '.data.externalReference')
curl -fsS -X POST "$API/requests/$ID/assign" -H "Authorization: Bearer $AGENT" -H 'Content-Type: application/json' \
  -d '{"assignedTo":"agent"}' > /dev/null
for i in $(seq 1 45); do
  curl -fsS "$API/notifications" -H "Authorization: Bearer $AGENT" | jq -e --arg ref "$REF" '.data[] | select(.message | contains($ref))' > /dev/null && break
  [ "$i" = 45 ] && fail "no notification arrived: the event did not make it through Kafka"
  sleep 2
done
echo "request $REF -> outbox -> Kafka -> consumer -> notification"

step "the dashboard numbers come from the event tables"
METRICS=$(curl -fsS "$API/analytics/dashboard" -H "Authorization: Bearer $AGENT")
echo "$METRICS" | jq -c '.data | {outboxPending, eventsPublished, eventsConsumed}'
[ "$(echo "$METRICS" | jq '.data.eventsPublished')" -gt 0 ] || fail "nothing was published"
[ "$(echo "$METRICS" | jq '.data.eventsConsumed')" -gt 0 ] || fail "nothing was consumed"

step "runbook search (pgvector)"
TOP=$(curl -fsS --get "$API/knowledge/search" --data-urlencode "query=payment deducted but order still pending" -H "Authorization: Bearer $AGENT" | jq -r '.data[0].documentTitle')
echo "$TOP"
case "$TOP" in *Payment*) ;; *) fail "expected the payment runbook first";; esac

step "investigate, be refused the approval as an agent, approve as a manager"
ACTION=$(curl -fsS -X POST "$API/requests/$ID/investigate" -H "Authorization: Bearer $AGENT" | jq -r '.data.recommendedAction')
[ "$ACTION" = "REPLAY_PAYMENT_EVENT" ] || fail "unexpected recommendation: $ACTION"
APPROVAL=$(curl -fsS "$API/approvals/pending" -H "Authorization: Bearer $MANAGER" | jq --argjson id "$ID" '.data[] | select(.requestId == $id) | .id')
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$API/approvals/$APPROVAL/approve" -H "Authorization: Bearer $AGENT" -H 'Content-Type: application/json' -d '{}')
[ "$CODE" = 403 ] || fail "an agent approving should get 403, got $CODE"
curl -fsS -X POST "$API/approvals/$APPROVAL/approve" -H "Authorization: Bearer $MANAGER" -H 'Content-Type: application/json' -d '{"comment":"smoke test"}' > /dev/null
STATUS=$(curl -fsS "$API/requests/$ID" -H "Authorization: Bearer $AGENT" | jq -r '.data.status')
[ "$STATUS" = "RESOLVED" ] || fail "expected RESOLVED, got $STATUS"
echo "$ACTION recommended -> 403 for the agent -> approved by the manager -> $STATUS"

# Only where this script can control the containers (CI sets WITH_OUTAGE=1).
if [ "${WITH_OUTAGE:-0}" = 1 ]; then
  step "Kafka goes away: the API keeps working and the event waits in the outbox"
  pending() { curl -fsS "$API/analytics/dashboard" -H "Authorization: Bearer $AGENT" | jq '.data.outboxPending'; }
  docker compose stop kafka > /dev/null 2>&1
  curl -fsS -X POST "$API/requests" -H "Authorization: Bearer $AGENT" -H 'Content-Type: application/json' \
    -d '{"title":"Created while Kafka was down","description":"The API must accept this and keep its event until the broker is back."}' > /dev/null
  sleep 12
  WAITING=$(pending)
  [ "$WAITING" -gt 0 ] || fail "with Kafka down the event should be waiting, but the backlog is $WAITING"
  echo "request accepted; outbox backlog while Kafka is down: $WAITING"

  step "Kafka comes back: the backlog drains and nothing is lost"
  docker compose start kafka > /dev/null 2>&1
  for i in $(seq 1 60); do
    WAITING=$(pending)
    [ "$WAITING" = 0 ] && break
    [ "$i" = 60 ] && fail "the backlog did not drain after Kafka came back (still $WAITING)"
    sleep 3
  done
  echo "backlog back to 0"
fi

step "the login rate limit (Redis)"
LAST=200
for i in $(seq 1 12); do
  LAST=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$API/auth/login" -H 'Content-Type: application/json' -d '{"username":"agent","password":"wrong"}')
done
[ "$LAST" = 429 ] || fail "expected 429 after repeated logins, got $LAST"
echo "429 after repeated attempts"

printf '\nAll checks passed.\n'
