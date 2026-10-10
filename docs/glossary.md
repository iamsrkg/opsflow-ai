# Glossary

**ACID** — Atomicity, Consistency, Isolation, Durability.

**Aggregate** — A domain consistency boundary.

**At-least-once delivery** — A message may be delivered more than once; consumers must tolerate duplicates.

**Cache-aside** — Application reads cache first and populates it from the source of truth on a miss.

**Consumer group** — Kafka consumers that share partition processing.

**DLT** — Dead-letter topic for messages that cannot be processed normally.

**Idempotency** — Repeating the same logical operation does not create additional unintended effects.

**Outbox** — Durable database record representing an event that must eventually be published.

**Optimistic locking** — Detect conflicts using a version instead of holding a DB lock throughout the operation.

**RAG** — Retrieval-Augmented Generation.

**pgvector** — PostgreSQL extension for vector similarity search.

**RBAC** — Role-Based Access Control.

**SLO** — Service Level Objective.

**Correlation ID** — Identifier used to connect logs/events belonging to one logical request.

**Backpressure** — Mechanism for preventing fast producers from overwhelming slower consumers.
