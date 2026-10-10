# Test Plan

| Layer | Examples | Goal |
|---|---|---|
| Unit | workflow, classification, permissions | deterministic correctness |
| Integration | PostgreSQL, Redis, Kafka | infrastructure behavior |
| API | auth, request, AI, approval | contract correctness |
| E2E | request -> event -> AI -> approval | business flow |
| Failure | broker down, duplicate event, stale version | resilience |
| Load | k6 | measured performance |
| AI eval | synthetic labeled set | retrieval/classification quality |
| Security | authz bypass, injection, secrets | security confidence |
