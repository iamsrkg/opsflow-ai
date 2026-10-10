# OpsFlow AI — API Contract Documentation

Base URL: `/api/v1`

All requests and responses use standard JSON formatting wrapped in `ApiResponse<T>`:

```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { ... },
  "correlationId": "CORR-884920A1",
  "timestamp": "2026-10-10T14:45:00"
}
```

---

## 1. Identity & Authentication (`/api/v1/auth`)

### `POST /api/v1/auth/login`
- **Request Body:**
```json
{
  "username": "operator",
  "password": "operator123"
}
```
- **Response `200 OK`:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "user": {
      "id": 4,
      "username": "operator",
      "fullName": "Op User",
      "email": "operator@opsflow.ai",
      "role": "OPERATOR"
    }
  }
}
```

---

## 2. Request Management (`/api/v1/requests`)

### `POST /api/v1/requests`
- **Request Body:**
```json
{
  "title": "Charged ₹500 but order payment pending",
  "description": "Customer charged at 10:31 AM, Txn #TXN-99482104, order status remains PENDING.",
  "category": "PAYMENT",
  "priority": "HIGH"
}
```

### `GET /api/v1/requests?page=0&size=10&status=INVESTIGATING`
- Paginated search with filtering by Category, Priority, Status, and search query.

### `POST /api/v1/requests/{id}/transition`
- **Request Body:**
```json
{
  "targetStatus": "INVESTIGATING",
  "comment": "Assigned operator initiated investigation"
}
```

---

## 3. AI Intelligence & Investigation (`/api/v1/requests/{id}`)

### `POST /api/v1/requests/{id}/classify`
- Classifies title/description into `category`, `priority`, `summary`, and `confidenceScore`.

### `POST /api/v1/requests/{id}/investigate`
- Triggers multi-step AI reasoning over audit logs, Kafka consumer lag, and pgvector runbook embeddings.
- Returns evidence breakdown, likely root cause, and recommended action.

---

## 4. Human-in-the-Loop Approvals (`/api/v1/approvals`)

### `GET /api/v1/approvals/pending`
- Fetches all pending sensitive actions requiring Manager/Admin authorization.

### `POST /api/v1/approvals/{id}/approve`
- Authorizes and deterministically executes backend action (e.g. `REPLAY_PAYMENT_EVENT`).
