# cURL Demo

Set:
```bash
export TOKEN='...'
```

Create an idempotent request:
```bash
curl -X POST http://localhost:8080/api/v1/requests \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -H 'X-Idempotency-Key: payment-demo-001' \
  -d '{"title":"Payment charged but order pending","description":"I was charged but my order still says payment pending.","category":"PAYMENT","priority":"HIGH"}'
```

Classify:
```bash
curl -X POST http://localhost:8080/api/v1/ai/requests/<REQUEST_ID>/classify -H "Authorization: Bearer $TOKEN"
```

Investigate:
```bash
curl -X POST http://localhost:8080/api/v1/ai/requests/<REQUEST_ID>/investigate -H "Authorization: Bearer $TOKEN"
```

Request approval:
```bash
curl -X POST http://localhost:8080/api/v1/approvals/requests/<REQUEST_ID> \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"action":"REVIEW_AND_RESOLVE"}'
```
