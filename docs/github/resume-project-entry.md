# Resume / GitHub Project Entry

## Recommended project title

**OpsFlow AI — AI-Powered Distributed Business Operations & Workflow Platform**

## One-line description

AI-powered, event-driven operations platform that manages deterministic workflows, reliable asynchronous events, organizational knowledge and evidence-backed AI investigations with human-controlled sensitive actions.

## Resume bullets

- Designed and implemented an **AI-powered operations workflow platform** using Java 21, Spring Boot, PostgreSQL, Kafka, Redis and React, supporting deterministic request lifecycles, auditability and asynchronous event processing.
- Implemented a **transactional outbox + Kafka** architecture with versioned event envelopes, correlation IDs, manual acknowledgements, retries, DLT handling and idempotent event consumption to handle at-least-once delivery safely.
- Built **JWT/RBAC security, API idempotency, optimistic locking, Redis cache-aside and distributed rate limiting**, protecting workflow state and reducing duplicate/concurrent mutation risks.
- Added **RAG-based AI classification and investigation using pgvector**, with evidence/hypothesis separation, provider abstraction and a human-in-the-loop approval boundary so AI recommendations cannot directly execute protected actions.
- Added Docker/Kubernetes/Terraform starters, Actuator/Micrometer observability, automated testing strategy and k6 load-testing methodology; documented production evolution for migrations, tracing, tenant isolation and high-availability scaling.

## Interview-safe wording

Say “implemented” only for behavior that exists in the repository. Say “designed for” or “production evolution” for capabilities documented but not fully implemented.

## LinkedIn/GitHub README summary

> OpsFlow AI is a backend-first operations control plane demonstrating reliable distributed systems and practical AI. It combines deterministic workflows, PostgreSQL transactions, transactional outbox, Kafka, Redis, JWT/RBAC, pgvector RAG, AI provider abstraction, human approvals and observability. The project intentionally treats AI as advisory and keeps authorization and sensitive actions under deterministic backend control.
