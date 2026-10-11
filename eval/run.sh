#!/usr/bin/env bash
# Measures the two things the AI step does, against a stack that is already up:
#   1. retrieval: does runbook search return the right runbook for a question?   (eval/retrieval.json)
#   2. recommendation: does an investigation recommend the right action?        (eval/recommendations.json)
#
# The questions are written in different words from the runbooks, the way a customer would
# write, so the score shows whether search works on meaning or only on shared words.
#
# Usage:  bash eval/run.sh [base-url]        (default http://localhost:3000)
set -euo pipefail

BASE="${1:-http://localhost:3000}"
API="$BASE/api/v1"
HERE="$(cd "$(dirname "$0")" && pwd)"
OUT="${OUT:-$HERE/results}"
mkdir -p "$OUT"

echo "waiting for the backend..."
for i in $(seq 1 150); do
  [ "$(curl -fsS "$BASE/actuator/health" 2>/dev/null | jq -r '.status' 2>/dev/null)" = "UP" ] && break
  sleep 2
done
for i in $(seq 1 30); do
  TOKEN=$(curl -fsS -X POST "$API/auth/login" -H 'Content-Type: application/json' \
            -d '{"username":"agent","password":"agent123"}' 2>/dev/null | jq -r '.data.accessToken' 2>/dev/null) || true
  [ -n "${TOKEN:-}" ] && [ "$TOKEN" != null ] && break
  sleep 2
done
[ -n "${TOKEN:-}" ] && [ "$TOKEN" != null ] || { echo "could not sign in" >&2; exit 1; }
AUTH="Authorization: Bearer $TOKEN"

# ------------------------------------------------------------------ retrieval
echo
echo "== retrieval"
: > "$OUT/retrieval.jsonl"
COUNT=$(jq length "$HERE/retrieval.json")
for i in $(seq 0 $((COUNT - 1))); do
  QUERY=$(jq -r ".[$i].query" "$HERE/retrieval.json")
  EXPECTED=$(jq -r ".[$i].expected" "$HERE/retrieval.json")
  HITS=$(curl -fsS -G "$API/knowledge/search" --data-urlencode "query=$QUERY" --data-urlencode "limit=3" -H "$AUTH")
  FOUND=$(echo "$HITS" | jq -c '[.data[].documentTitle]')
  SCORES=$(echo "$HITS" | jq -c '[.data[].score]')
  # position of the expected runbook in the top three, 0 if it is not there
  RANK=$(echo "$FOUND" | jq --arg e "$EXPECTED" '(index($e) // -1) + 1')
  printf '  %s  %s\n' "$([ "$RANK" = 1 ] && echo "ok  " || echo "rank $RANK")" "$QUERY"
  jq -nc --arg q "$QUERY" --arg e "$EXPECTED" --argjson found "$FOUND" --argjson scores "$SCORES" --argjson rank "$RANK" \
    '{query: $q, expected: $e, found: $found, scores: $scores, rank: $rank}' >> "$OUT/retrieval.jsonl"
done

# ------------------------------------------------------------------ recommendation
echo
echo "== recommendation"
: > "$OUT/recommendations.jsonl"
COUNT=$(jq length "$HERE/recommendations.json")
for i in $(seq 0 $((COUNT - 1))); do
  BODY=$(jq -c ".[$i] | {title, description}" "$HERE/recommendations.json")
  EXPECTED=$(jq -r ".[$i].expected" "$HERE/recommendations.json")
  ID=$(curl -fsS -X POST "$API/requests" -H "$AUTH" -H 'Content-Type: application/json' -d "$BODY" | jq -r '.data.id')
  START=$(date +%s%3N)
  RESULT=$(curl -fsS -X POST "$API/requests/$ID/investigate" -H "$AUTH" --max-time 300)
  MILLIS=$(( $(date +%s%3N) - START ))
  ACTION=$(echo "$RESULT" | jq -r '.data.recommendedAction')
  BY=$(echo "$RESULT" | jq -r '.data.investigatedBy')
  RUNBOOK=$(echo "$RESULT" | jq -r '.data.relevantRunbookTitle // "none"')
  printf '  %s  %-30s -> %s  (%s ms)\n' "$([ "$ACTION" = "$EXPECTED" ] && echo "ok  " || echo "MISS")" "$(jq -r ".[$i].title" "$HERE/recommendations.json")" "$ACTION" "$MILLIS"
  jq -nc --argjson case "$BODY" --arg e "$EXPECTED" --arg a "$ACTION" --arg by "$BY" --arg rb "$RUNBOOK" --argjson ms "$MILLIS" \
    '{title: $case.title, expected: $e, action: $a, correct: ($a == $e), investigatedBy: $by, runbook: $rb, millis: $ms}' >> "$OUT/recommendations.jsonl"
done

# ------------------------------------------------------------------ summary
SETUP=$(jq -rs '.[0].investigatedBy | sub("^[^ ]+ via "; "")' "$OUT/recommendations.jsonl")
jq -n --arg setup "$SETUP" \
  --slurpfile r <(jq -s . "$OUT/retrieval.jsonl") --slurpfile c <(jq -s . "$OUT/recommendations.jsonl") '
  def pct(n; d): if d == 0 then 0 else (n / d * 1000 | round) / 10 end;
  ($r[0]) as $r | ($c[0]) as $c |
  {
    recommendedBy: $setup,
    retrieval: {
      questions: ($r | length),
      top1: ($r | map(select(.rank == 1)) | length),
      top3: ($r | map(select(.rank >= 1)) | length),
      recallAt1Percent: pct($r | map(select(.rank == 1)) | length; $r | length),
      recallAt3Percent: pct($r | map(select(.rank >= 1)) | length; $r | length),
      meanReciprocalRank: (($r | map(if .rank >= 1 then 1 / .rank else 0 end) | add) / ($r | length) * 1000 | round / 1000)
    },
    recommendation: {
      cases: ($c | length),
      correct: ($c | map(select(.correct)) | length),
      accuracyPercent: pct($c | map(select(.correct)) | length; $c | length),
      answeredByFallback: ($c | map(select(.investigatedBy | contains("fallback"))) | length),
      medianMillis: ($c | map(.millis) | sort | .[length / 2 | floor]),
      maxMillis: ($c | map(.millis) | max)
    }
  }' | tee "$OUT/result.json"
