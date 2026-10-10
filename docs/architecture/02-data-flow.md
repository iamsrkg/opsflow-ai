# Critical Data Flows

## Create request
1. Authenticate JWT.
2. Validate payload.
3. Apply rate limit.
4. Persist Request + AuditEvent + OutboxEvent in one DB transaction.
5. Outbox publisher sends event to Kafka.
6. Consumers process event idempotently.
7. AI classification can enrich the request asynchronously or on demand.

## AI investigation
1. User invokes investigation.
2. Backend loads request and audit history.
3. Retrieval searches pgvector knowledge embeddings.
4. Controlled tools expose only backend-approved evidence.
5. AI provider receives evidence, not unrestricted database access.
6. Response separates evidence, hypotheses, recommendations, and confidence.
7. Sensitive actions are routed through approval and deterministic backend authorization.

## Failure: DB succeeds, Kafka fails
The request and outbox row commit together. Kafka publication can fail temporarily; the scheduled publisher retries the unpublished outbox row.

## Failure: Kafka redelivers
ProcessedEvent keyed by event ID prevents duplicate side effects.
