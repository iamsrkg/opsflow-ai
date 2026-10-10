# API and Event Catalog

## Authentication

### POST `/api/v1/auth/register`

```json
{
  "username": "sudheer",
  "password": "strong-password-123"
}
```

### POST `/api/v1/auth/login`

```json
{
  "username": "sudheer",
  "password": "strong-password-123"
}
```

Returns an access token.

## Requests

### POST `/api/v1/requests`

Headers:

```text
Authorization: Bearer <JWT>
X-Idempotency-Key: create-request-001
X-Correlation-Id: optional-client-correlation-id
```

Body:

```json
{
  "title": "Payment pending after charge",
  "description": "I was charged but my order is still payment pending.",
  "category": "PAYMENT",
  "priority": "HIGH"
}
```

### GET `/api/v1/requests`

Lists requests.

### GET `/api/v1/requests/{id}`

Reads one request. The implementation uses Redis cache-aside.

### GET `/api/v1/requests/{id}/history`

Returns audit history.

### POST `/api/v1/requests/{id}/transition?status=INVESTIGATING`

Performs a backend-validated workflow transition.

### POST `/api/v1/requests/{id}/assign?assignee=operator1`

Assigns a request.

## AI

### POST `/api/v1/ai/requests/{id}/classify`

Runs AI classification and persists the resulting classification fields.

### POST `/api/v1/ai/requests/{id}/investigate`

Loads request/history, retrieves knowledge, invokes the AI provider and returns a structured investigation.

## Knowledge

### POST `/api/v1/knowledge`

Manager/admin operation to create a knowledge document and its vector embedding.

### GET `/api/v1/knowledge/search?q=payment+pending&limit=5`

Semantic retrieval endpoint.

## Approvals

### POST `/api/v1/approvals/requests/{id}`

Creates an approval request.

```json
{
  "action": "EXECUTE_PAYMENT_RECONCILIATION"
}
```

### POST `/api/v1/approvals/{id}/decide?approve=true`

Manager/admin decision.

### POST `/api/v1/approvals/{id}/execute`

Authorized operator/admin execution after approval.

## Event catalog

| Event | Meaning | Aggregate |
|---|---|---|
| `REQUEST_CREATED` | New request persisted | REQUEST |
| `REQUEST_CLASSIFIED` | AI classification persisted | REQUEST |
| `REQUEST_STATUS_CHANGED` | Workflow status changed | REQUEST |
| `REQUEST_ASSIGNED` | Request assigned | REQUEST |

## Event reliability fields

Every event should be explainable through:

- event ID;
- event type;
- aggregate ID;
- correlation ID;
- timestamp;
- schema version;
- payload.
