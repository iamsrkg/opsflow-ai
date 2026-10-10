# OpsFlow AI — System Design

## Goal
A backend-first operations control plane that manages requests, deterministic workflows, asynchronous events, operational knowledge, and grounded AI investigation.

## Architecture
```text
React UI / API clients
        |
   Spring Boot API
  /   |    |     \
Auth Request AI Approval
 |     |     |       |
JWT   JPA  RAG/tools  Human approval
       |      |
   PostgreSQL <--- pgvector
       |
 Transactional Outbox
       |
      Kafka ---> consumers / notifications / AI classification
       |
      Redis ---> cache / rate limiting / short-lived coordination

Actuator + Micrometer + correlation IDs ---> metrics/logs/troubleshooting
```

## Architectural principles
1. Deterministic backend owns authorization and state transitions.
2. AI is advisory; it cannot directly mutate protected business state.
3. Human approval is required for sensitive actions.
4. Database and event publication use an outbox to avoid dual-write inconsistency.
5. Consumers are idempotent because Kafka delivery is at-least-once.
6. Start modular-monolith-first; extract services only when scale or ownership justifies it.
7. Every important request/event carries correlation context for diagnosis.

## Why modular monolith first?
A small team gets clear domain boundaries without the operational cost of many services. The module boundaries are explicit so Request, AI, Knowledge, Approval, and Eventing can later become services if independent deployment/scaling is justified.
