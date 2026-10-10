# OpsFlow AI — Event Contract Documentation

All asynchronous Kafka domain events follow the `DomainEvent<T>` envelope format.

## 1. Domain Event Envelope Specification

```json
{
  "eventId": "EVT-99482104-A1",
  "eventType": "REQUEST_CREATED",
  "aggregateType": "REQUEST",
  "aggregateId": "1001",
  "correlationId": "CORR-884920A1",
  "timestamp": "2026-10-10T14:45:00",
  "data": { ... }
}
```

---

## 2. Event Topics

| Topic Name | Producer Service | Description | Consumer Group |
|---|---|---|---|
| `opsflow.request.events` | OutboxPublisher | Triggered when requests are created, classified, assigned, or status updated | `opsflow-classification-group`, `opsflow-audit-group` |
| `opsflow.investigation.events` | OutboxPublisher | Triggered when AI completes multi-step reasoning | `opsflow-notification-group` |
| `opsflow.approval.events` | OutboxPublisher | Triggered when sensitive action approval is requested or granted | `opsflow-execution-group` |

---

## 3. Transactional Outbox Schema

Table: `outbox_events`

```sql
CREATE TABLE outbox_events (
    id BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    correlation_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    error_message TEXT
);
```

---

## 4. Consumer Idempotency Table Schema

Table: `processed_events`

```sql
CREATE TABLE processed_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    consumer_group VARCHAR(255) NOT NULL,
    event_type VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_event_consumer UNIQUE (event_id, consumer_group)
);
```
