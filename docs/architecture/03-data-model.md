# Data Model

## Logical entities

```mermaid
erDiagram
    USER_ACCOUNT ||--o{ REQUEST : creates
    REQUEST ||--o{ AUDIT_EVENT : has
    REQUEST ||--o{ APPROVAL : requires
    REQUEST ||--o{ IDEMPOTENCY_RECORD : protects
    REQUEST ||--o{ OUTBOX_EVENT : emits
    KNOWLEDGE_DOCUMENT ||--o{ VECTOR : embeds
    OUTBOX_EVENT ||--o{ PROCESSED_EVENT : consumed_as

    USER_ACCOUNT {
      uuid id PK
      string username UK
      string password_hash
      string role
    }
    REQUEST {
      uuid id PK
      string title
      text description
      string category
      string priority
      string status
      string assignee
      long version
      timestamp created_at
      timestamp updated_at
    }
    AUDIT_EVENT {
      uuid id PK
      uuid request_id
      string actor
      string event_type
      string previous_state
      string new_state
      timestamp created_at
    }
    OUTBOX_EVENT {
      uuid id PK
      string event_type
      uuid aggregate_id
      text payload
      boolean published
      timestamp created_at
    }
    PROCESSED_EVENT {
      uuid event_id PK
      string consumer_name
      timestamp processed_at
    }
    APPROVAL {
      uuid id PK
      uuid request_id
      string action
      string status
      string decided_by
    }
    IDEMPOTENCY_RECORD {
      uuid id PK
      string actor
      string idempotency_key
      uuid request_id
    }
    KNOWLEDGE_DOCUMENT {
      uuid id PK
      string title
      text content
      string source
    }
```

## Source of truth

- Request state: PostgreSQL.
- Audit history: PostgreSQL.
- Outbox publication intent: PostgreSQL.
- Kafka: transport/event distribution, not authoritative request state.
- Redis: acceleration/short-lived coordination, not source of truth.
- pgvector: searchable knowledge representation, not authoritative business state.
- AI output: recommendation/analysis, not authoritative business state.

## Transaction boundary

For request creation:

```text
BEGIN
  request INSERT
  audit INSERT
  outbox INSERT
  idempotency INSERT (optional)
COMMIT
```

Kafka publication occurs after that transaction.

## Cache-aside behavior

Request reads first consult Redis. On a miss, PostgreSQL remains the source of truth; the response is then written to Redis with a short TTL. Mutations evict the request cache key after the database change. Redis failure is deliberately fail-open for this portfolio implementation so cache availability does not become a source-of-truth dependency.
