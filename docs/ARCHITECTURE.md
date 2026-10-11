# Architecture

One Spring Boot application, split into packages by what they are responsible for, with PostgreSQL, Kafka and Redis around it and a React dashboard in front.

```mermaid
graph TD
    Browser[React dashboard] -->|HTTPS, JWT| Nginx[nginx]
    Nginx -->|/api| App[Spring Boot application]

    subgraph App modules
        Identity[identity]
        Request[request]
        Workflow[workflow]
        Outbox[outbox]
        Notification[notification]
        AI[ai]
        Approval[approval]
        Audit[audit]
        Knowledge[knowledge]
        RateLimit[ratelimit]
    end

    App --> PG[(PostgreSQL + pgvector)]
    App --> Redis[(Redis)]
    Outbox -->|publish| Kafka[Kafka]
    Kafka -->|consume| Notification
```

## The path of one request

1. **Rate limit.** `RateLimitFilter` counts the call in Redis before anything else happens. Over the limit, the answer is `429` and no password hash or query is spent on it.
2. **Authentication.** `JwtAuthenticationFilter` checks the token and loads the user's role. Each controller method states which roles may call it.
3. **The change.** `RequestService` loads the request, asks `WorkflowStateMachine` whether the transition is allowed, and saves it. In the same transaction it writes a history row, an audit row and an outbox row.
4. **Publishing.** `OutboxPublisher` runs every two seconds and keeps taking batches of 50 pending rows until the outbox is empty. It sends each row to Kafka, waits for the broker to acknowledge it, and writes the batch's new statuses together.
5. **Consuming.** `NotificationConsumer` reads the event. `NotificationService` checks whether this event id was already handled, writes the notification and the "handled" marker in one transaction.

Steps 1 to 3 happen while the caller waits. Steps 4 and 5 happen afterwards, so a slow or unavailable Kafka never slows down or fails the API call.

## The life of a request, in sequence

Creating and assigning a request, and what follows from it:

```mermaid
sequenceDiagram
    autonumber
    actor Agent
    participant API as Spring Boot API
    participant DB as PostgreSQL
    participant Pub as OutboxPublisher
    participant K as Kafka
    participant C as NotificationConsumer

    Agent->>API: POST /requests/42/assign (JWT)
    API->>API: rate limit, authenticate, check role
    rect rgb(235, 245, 255)
    note over API,DB: one database transaction
    API->>DB: update request (version + 1)
    API->>DB: insert history row and audit row
    API->>DB: insert outbox row (PENDING, eventId)
    end
    API-->>Agent: 200, with X-Correlation-ID
    loop every 2 seconds
        Pub->>DB: read PENDING rows, oldest first
        Pub->>K: send (key = request id)
        K-->>Pub: acknowledgement
        Pub->>DB: mark PUBLISHED
    end
    K->>C: deliver event
    rect rgb(235, 245, 255)
    note over C,DB: one database transaction
    C->>DB: was this eventId handled by this group?
    C->>DB: insert notification
    C->>DB: insert processed-event row
    end
    C-->>K: offset committed
```

Investigating and approving:

```mermaid
sequenceDiagram
    autonumber
    actor Agent
    actor Manager
    participant API as Spring Boot API
    participant Tools as AIToolRegistry (read only)
    participant P as AI provider
    participant Policy as ActionPolicy

    Agent->>API: POST /requests/42/investigate
    API->>API: classify if never classified, move to INVESTIGATING
    API->>Tools: request, audit trail, events, closest runbooks
    Tools-->>API: evidence (facts from the database)
    API->>P: recommend(evidence)
    P-->>API: summary, likely cause, action, confidence
    API->>Policy: is the action known? does it need approval?
    alt needs approval
        API->>API: create approval, move to PENDING_APPROVAL
    else no approval needed
        API->>API: stay in INVESTIGATING
    end
    API-->>Agent: the investigation, with its evidence
    Manager->>API: POST /approvals/7/approve
    API->>API: role check (manager or admin only)
    API->>API: ACTION_IN_PROGRESS, then RESOLVED
    API-->>Manager: 200
```

## What happens at startup

1. **Flyway** runs any migration that hasn't run yet (PostgreSQL profile).
2. **Hibernate** validates every entity against the schema and stops the application if they disagree.
3. **`DataInitializer`** seeds the demo users, runbooks and sample requests if the tables are empty.
4. **`VectorStoreService`** gives every runbook chunk without an embedding one. Only after this does the application report ready, so `/actuator/health` is not UP while the index is still being built.
5. **Kafka**: if enabled, the topics and their dead-letter topics are created and the consumer starts; the outbox publisher begins polling.

## Modules

| Package | Responsibility |
|---|---|
| `identity` | Login, JWT creation and validation, users and roles |
| `request` | The request entity, its API, and the service that changes it |
| `workflow` | The table of allowed state transitions |
| `outbox` | The outbox table and the publisher |
| `events` | The event envelope, topic names, and the table of handled events |
| `notification` | The Kafka consumer and the notifications it writes |
| `ai` | Evidence collection, the tool registry, the action policy, and the two providers |
| `approval` | The queue of recommended actions waiting for a manager |
| `audit` | Who did what, with the correlation id |
| `knowledge` | Runbooks, embeddings, and the pgvector and in-memory indexes |
| `ratelimit` | The Redis counter and the filter that uses it |
| `analytics` | The numbers on the dashboard |

## Data

Flyway owns the PostgreSQL schema (`backend/src/main/resources/db/migration`). Hibernate is set to `validate`, so the application refuses to start if an entity and the schema disagree.

The tables that carry the design:

- `requests` has a `version` column for optimistic locking.
- `outbox_events` has a unique `event_id`, a `status` and an `attempts` counter.
- `processed_events` has a unique constraint on `(event_id, consumer_group)`. That constraint is what makes handling an event twice impossible, even if two consumers race.
- `knowledge_chunks` has an `embedding vector(256)` column (feature hashing) and an `embedding_model vector(384)` column (an embedding model), each with an HNSW index for cosine distance, and `embedding_model_name` to record which model made the vector.

## Two ways to run it

| | In-memory profile (default) | `prod` profile |
|---|---|---|
| Database | H2, tables created by Hibernate | PostgreSQL, schema from Flyway |
| Runbook search | vectors compared in Java | pgvector |
| Kafka | off unless `KAFKA_ENABLED=true`; events wait in the outbox | on in Docker Compose |
| Rate limiting | off unless `RATE_LIMIT_ENABLED=true` | on in Docker Compose |

The default profile exists so the backend starts with nothing else installed. The same code paths run in both; only the storage behind `VectorIndex` differs.
