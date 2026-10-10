# Decisions

The choices that shaped the code, with what each one costs.

## One application, split into modules

**Decision.** A single Spring Boot application with one package per responsibility, not a set of services.

**Why.** The workflow needs a request, its history, its audit row and its event to change together. Inside one application that is one database transaction. Across services it would need a saga for what is, here, a simple write.

**Cost.** Everything is deployed and scaled together. The packages only talk through services, so a module could be moved out later, starting with the Kafka consumer, which already depends on nothing but the event.

## A transactional outbox, not a direct publish

**Decision.** A change writes its event to an `outbox_events` table in the same transaction. A separate publisher sends the table's pending rows to Kafka.

**Why.** Saving to the database and publishing to Kafka are two systems, and one of them can fail after the other succeeded. Publish first and the event can describe a change that was rolled back. Save first and a crash before the publish loses the event. With the outbox there is only one write, to one database, and it either happens or it doesn't.

**Cost.** Events arrive up to a polling interval late (two seconds), and the same event can be delivered more than once: if the application stops after Kafka acknowledged events but before their batch was marked as published, up to one batch of 50 is sent again on restart. That is why the consumers are idempotent.

**Known limit.** The publisher assumes it is the only one. Two instances would both read the same pending rows. Running several needs each one to claim rows with `SELECT … FOR UPDATE SKIP LOCKED`.

## Mark as published only after the broker's acknowledgement

**Decision.** `OutboxPublisher` waits for the result of each send. On failure the row stays pending, and the rest of the batch waits behind it. Statuses are written once per batch, not once per row: the load test showed that a database write per event made the publisher queue for a connection behind the API, and held it to about 25 events a second.

**Why.** `KafkaTemplate.send` returns before the broker has the record. Marking the row at that point would report events as sent while Kafka was down. Stopping the batch at the first failure keeps a request's events in the order they happened.

**Cost.** One event that keeps failing holds up the ones behind it. Only a broker problem causes that, and then nothing could be sent anyway. An event that can't even be serialized is marked failed and skipped, because retrying would never fix it.

## Idempotent consumers and a dead-letter topic

**Decision.** Every event has an id. A consumer records the ids it has handled, in the same transaction as its work, under a unique constraint on `(event_id, consumer_group)`. A message that still fails after two retries goes to `<topic>.DLT`.

**Why.** Kafka with an outbox gives at-least-once delivery, so handling an event twice has to be harmless. The unique constraint, not an in-memory check, is what guarantees it: if two consumers handled the same event at the same moment, one transaction would fail and roll its work back.

**Cost.** One extra row per event per consumer group. Messages on the dead-letter topic need someone to look at them; nothing replays them automatically.

## Events keyed by aggregate id

**Decision.** The Kafka message key is the request id.

**Why.** Kafka keeps order within a partition, and the same key always goes to the same partition. So "created" is always read before "assigned" for one request, while different requests are spread across partitions and can be consumed in parallel.

## Optimistic locking with a version the client sends

**Decision.** `requests.version` is a JPA `@Version`. A transition may include the `expectedVersion` the client read.

**Why.** Two people open the same request, both act on what they saw. Without a check the second write silently wins. With it the second caller gets `409` and reloads. Locking the row while a person looks at it would block everyone else for as long as they think.

**Cost.** The client has to handle the conflict.

## The AI recommends; code decides

**Decision.** The AI step gets its facts from `AIToolRegistry`, which only reads. Its recommendation is checked against `ActionPolicy`, which lists the actions that exist and which of them need a manager. Nothing is executed without an approval.

**Why.** A language model's output is untrusted input. A request's description is written by a customer and ends up in the prompt, so it could contain instructions. Because the model has no tools and its answer can only be one of a fixed set of actions, the worst a manipulated answer can do is suggest a known action, which a person then reviews.

**Cost.** The model can't discover anything the backend didn't collect for it.

**Fallback.** If the model is unreachable or its answer can't be parsed, the rule-based provider answers. The workflow never waits on an outside service.

## pgvector inside PostgreSQL

**Decision.** Embeddings live in a `vector(256)` column of the table that holds the text, with an HNSW index.

**Why.** There are few runbooks and they already live in PostgreSQL. A separate vector database would be one more thing to run and to keep in sync. HNSW was chosen over IVFFlat because it needs no training step and keeps its recall as rows are added.

**The honest part.** The embedding is feature hashing: words and word pairs hashed into 256 slots. It is deterministic and needs no API, and it ranks by shared vocabulary only. It does not know that "refund" and "reimbursement" are related. `EmbeddingService` is the one class to replace for a learned model, along with the dimension in the migration.

## A fixed-window rate limiter in Redis

**Decision.** One counter per client per minute, incremented and given its expiry in a single Lua script. Checked before authentication. Login has a lower limit. Redis down means requests pass.

**Why Redis.** A counter in the application's memory is per instance, so three instances would allow three times the limit.

**Why Lua.** `INCR` followed by `EXPIRE` as two commands can be interrupted between them, leaving a counter that never expires.

**Why before authentication.** Checking a password is deliberately slow. Rejecting a flood before that step is the point.

**Cost of fixed windows.** A client can send a full window's worth at the end of one minute and again at the start of the next. A sliding window or a token bucket would smooth that, at the price of more state per client.

**Cost of failing open.** With Redis down there is no limit. The alternative, failing closed, would turn a Redis outage into an API outage.

## Flyway owns the schema

**Decision.** Migrations in SQL; Hibernate set to `validate`.

**Why.** With `ddl-auto=update` the schema is whatever Hibernate decided at the last start, with no record and no review. A migration is a file in version control, and `validate` fails the start when an entity and the schema drift apart.

## Tests against the real things

**Decision.** The Kafka path is tested against an in-process broker, PostgreSQL and Redis against containers, and the whole stack through Docker Compose in CI.

**Why.** The behaviours that matter here, a unique constraint rejecting a duplicate, a dead-letter publish, a Lua script, a pgvector operator, don't exist in a mock. The first run of the end-to-end test found a request that was never classified, which no unit test had caught.

**Cost.** The suite takes about a minute, and the container tests need Docker.
