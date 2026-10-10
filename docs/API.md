# API

Base path `/api/v1`. Every response has the same shape and carries an `X-Correlation-ID` header:

```json
{ "success": true, "message": "…", "data": { }, "correlationId": "CORR-1A2B3C4D", "timestamp": "…" }
```

Everything except login and registration needs `Authorization: Bearer <token>`.

## Roles

`ADMIN`, `MANAGER`, `AGENT`, `OPERATOR`, `VIEWER`. A viewer can only read. Only a manager or an admin can approve or reject. A new account created through `/auth/register` is always a viewer.

## Endpoints

| Method | Path | Who | What it does |
|---|---|---|---|
| POST | `/auth/login` | anyone | `{username, password}` → `{accessToken, user}`. `401` on wrong credentials |
| POST | `/auth/register` | anyone | Creates a viewer account |
| GET | `/auth/me` | signed in | The current user |
| POST | `/requests` | any role | `{title, description, category?, priority?}` → the new request, in `CREATED` |
| GET | `/requests` | any role | Paged list. Filters: `status`, `category`, `priority`, `query`; `page`, `size`, `sortBy`, `sortDir` |
| GET | `/requests/{id}` | any role | One request |
| GET | `/requests/{id}/history` | any role | Its status changes |
| POST | `/requests/{id}/transition` | not viewer | `{targetStatus, comment?, expectedVersion?}`. `409` if the workflow doesn't allow it, or if `expectedVersion` is stale |
| POST | `/requests/{id}/assign` | not viewer | `{assignedTo}` |
| POST | `/requests/{id}/escalate` | not viewer | `{reason}`; priority becomes `CRITICAL` |
| POST | `/requests/{id}/classify` | not viewer | Classifies the request (category, priority, summary) |
| POST | `/requests/{id}/investigate` | not viewer | Collects evidence and stores a recommendation. If the action needs approval, the request moves to `PENDING_APPROVAL` |
| GET | `/requests/{id}/investigations` | any role | Past investigations of the request |
| GET | `/approvals/pending` | any role | Actions waiting for a decision |
| POST | `/approvals/{id}/approve` | manager, admin | `{comment?}`. The request is resolved |
| POST | `/approvals/{id}/reject` | manager, admin | `{reason?}`. The request goes back to `INVESTIGATING` |
| GET | `/knowledge/documents` | any role | The runbooks |
| GET | `/knowledge/search?query=…&limit=5` | any role | Runbook chunks ranked by similarity, each with a score from 0 to 1 |
| GET | `/audit/events` | any role | Paged audit trail, newest first |
| GET | `/audit/requests/{requestId}` | any role | The audit trail of one request |
| GET | `/notifications` | any role | The latest 50 notifications written by the Kafka consumer |
| GET | `/analytics/dashboard` | any role | Counts, including `outboxPending`, `eventsPublished`, `eventsConsumed` |

Outside `/api/v1`: `GET /actuator/health` is public.

## Status codes

| Code | When |
|---|---|
| 400 | A required field is missing or invalid |
| 401 | Wrong username or password |
| 403 | No token, or the role may not do this |
| 404 | No such request or approval |
| 409 | The workflow doesn't allow the transition, or the request changed since it was read |
| 429 | Rate limit reached; see `Retry-After` |

## Events

Published to Kafka, keyed by the aggregate id:

| Topic | Event types |
|---|---|
| `opsflow.request.events` | `REQUEST_CREATED`, `REQUEST_CLASSIFIED`, `REQUEST_ASSIGNED`, `REQUEST_TRANSITIONED`, `REQUEST_ESCALATED` |
| `opsflow.investigation.events` | `INVESTIGATION_COMPLETED` |
| `opsflow.approval.events` | `APPROVAL_REQUESTED`, `APPROVAL_APPROVED` |

Each message is one JSON envelope:

```json
{
  "eventId": "a UUID, the same on every redelivery",
  "eventType": "REQUEST_ASSIGNED",
  "aggregateType": "REQUEST",
  "aggregateId": "42",
  "correlationId": "CORR-1A2B3C4D",
  "timestamp": "2026-10-11T00:38:15",
  "data": { "the aggregate as it was after the change": "…" }
}
```

A message that can't be handled ends up on the topic of the same name with `.DLT` appended.
