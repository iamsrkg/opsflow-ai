# ADR-004: Idempotent Mutations

Mutation endpoints may be retried by clients or gateways. `X-Idempotency-Key` is stored per actor with the resulting request ID under a unique constraint. A repeated key returns the original request instead of creating a duplicate.

For production, add TTL/retention and store enough response metadata to reproduce the original response exactly.
