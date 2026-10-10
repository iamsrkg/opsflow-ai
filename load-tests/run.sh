#!/usr/bin/env bash
# Runs the load test against a stack that is already up, and measures two things:
#   1. the API under load (k6: requests per second, latency percentiles, failures)
#   2. the event pipeline: how fast the outbox publishes to Kafka and how fast the consumer keeps up
#
# Usage:  docker compose -f docker-compose.yml -f load-tests/docker-compose.loadtest.yml up --build -d
#         bash load-tests/run.sh
set -euo pipefail

BASE="${BASE:-http://localhost:3000}"
API="$BASE/api/v1"
VUS="${VUS:-50}"
OUT="${OUT:-load-tests/results}"
mkdir -p "$OUT" && chmod 777 "$OUT"

echo "waiting for the backend..."
for i in $(seq 1 90); do
  [ "$(curl -fsS "$BASE/actuator/health" 2>/dev/null | jq -r '.status' 2>/dev/null)" = "UP" ] && break
  sleep 2
done
for i in $(seq 1 30); do
  TOKEN=$(curl -fsS -X POST "$API/auth/login" -H 'Content-Type: application/json' \
            -d '{"username":"agent","password":"agent123"}' 2>/dev/null | jq -r '.data.accessToken' 2>/dev/null) || true
  [ -n "${TOKEN:-}" ] && [ "$TOKEN" != null ] && break
  sleep 2
done
metrics() { curl -fsS "$API/analytics/dashboard" -H "Authorization: Bearer $TOKEN" | jq -c '.data | {p: .eventsPublished, c: .eventsConsumed, w: .outboxPending}'; }

# let the seed data's events drain so the run starts from an empty outbox
for i in $(seq 1 60); do [ "$(metrics | jq '.w')" = 0 ] && break; sleep 1; done
BEFORE=$(metrics)
echo "before: $BEFORE"

# sample the pipeline once a second while the test runs and afterwards
( while true; do echo "$(date +%s) $(metrics 2>/dev/null || echo '{}')"; sleep 1; done ) > "$OUT/pipeline.log" &
SAMPLER=$!
trap 'kill $SAMPLER 2>/dev/null || true' EXIT

START=$(date +%s)
docker run --rm --network host -u "$(id -u):$(id -g)" -e BASE="$BASE" -e VUS="$VUS" \
  -v "$PWD/load-tests:/scripts:ro" -v "$PWD/$OUT:/results" grafana/k6:0.54.0 run --quiet /scripts/k6-load.js
LOAD_END=$(date +%s)

echo "load finished; waiting for the outbox to drain and the consumer to catch up..."
for i in $(seq 1 900); do
  NOW=$(metrics)
  [ "$(echo "$NOW" | jq '.w')" = 0 ] && [ "$(echo "$NOW" | jq '.p == .c')" = true ] && break
  sleep 1
done
DRAINED=$(date +%s)
kill $SAMPLER 2>/dev/null || true
AFTER=$(metrics)
echo "after: $AFTER"

EVENTS=$(( $(echo "$AFTER" | jq '.p') - $(echo "$BEFORE" | jq '.p') ))
CONSUMED=$(( $(echo "$AFTER" | jq '.c') - $(echo "$BEFORE" | jq '.c') ))
PEAK=$(jq -s 'map(.w // 0) | max' <(cut -d' ' -f2- "$OUT/pipeline.log"))
LOAD_SECONDS=$(( LOAD_END - START ))
TOTAL_SECONDS=$(( DRAINED - START ))
DRAIN_SECONDS=$(( DRAINED - LOAD_END ))

jq -n --slurpfile k6 "$OUT/k6.json" \
  --argjson events "$EVENTS" --argjson consumed "$CONSUMED" --argjson peak "$PEAK" \
  --argjson load "$LOAD_SECONDS" --argjson total "$TOTAL_SECONDS" --argjson drain "$DRAIN_SECONDS" \
  '{api: $k6[0],
    pipeline: {eventsPublished: $events, eventsConsumed: $consumed, peakOutboxBacklog: $peak,
               loadSeconds: $load, secondsToDrainAfterLoad: $drain,
               publishedPerSecond: (($events / $total * 10 | round) / 10)}}' | tee "$OUT/result.json"

[ "$EVENTS" = "$CONSUMED" ] || { echo "FAILED: $EVENTS events published but $CONSUMED consumed" >&2; exit 1; }
echo "every published event was consumed exactly once"
