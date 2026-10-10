# ADR-002: Transactional Outbox

Status: Accepted

The request write and event intent are stored in PostgreSQL in one transaction. A separate publisher sends the outbox event to Kafka. This prevents the classic dual-write case where a database transaction succeeds but event publication fails.

Trade-off: events may be delivered more than once, so consumers must be idempotent.
