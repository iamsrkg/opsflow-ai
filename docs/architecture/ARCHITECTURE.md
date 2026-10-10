# OpsFlow AI — Technical Architecture & System Design

## 1. System Vision & Design Philosophy

OpsFlow AI is a backend-first, event-driven business operations and workflow platform. AI operates as a controlled capability within a deterministic system rather than an unconstrained autonomous agent.

### Core Architectural Principles:
1. **AI Recommends, Backend Decides, Human Approves Sensitive Actions:** The LLM does not execute raw SQL or mutate database state directly.
2. **Graceful System Degradation:** If the LLM provider becomes unreachable, core business functions (request creation, workflow state machine, auth, assignments, audit logging) continue operating cleanly.
3. **Transactional Outbox Pattern:** Guarantees zero message loss between PostgreSQL transactional persistence and Apache Kafka event publishing.
4. **Idempotency & Retry/DLQ:** Consumer idempotency tracked via `ProcessedEvent` table to prevent duplicate message handling during network retries.

---

## 2. Component Topology

```
                  ┌─────────────────────────────────┐
                  │      React / TypeScript UI      │
                  │   Operations & AI Dashboard     │
                  └────────────────┬────────────────┘
                                   │ HTTP / REST (JWT)
                                   ▼
                  ┌─────────────────────────────────┐
                  │    Spring Boot 3.3.x Backend    │
                  │                                 │
                  │  ┌────────────┐ ┌────────────┐  │
                  │  │ Identity   │ │ Requests   │  │
                  │  └────────────┘ └────────────┘  │
                  │  ┌────────────┐ ┌────────────┐  │
                  │  │ Workflow   │ │ AI RAG     │  │
                  │  └────────────┘ └────────────┘  │
                  │  ┌────────────┐ ┌────────────┐  │
                  │  │ Outbox     │ │ Approval   │  │
                  │  └────────────┘ └────────────┘  │
                  └────────┬───────────────┬────────┘
                           │               │
            ┌──────────────┘               └──────────────┐
            ▼                                             ▼
  ┌──────────────────┐                           ┌──────────────────┐
  │ PostgreSQL 16    │                           │ Redis 7          │
  │ + pgvector       │                           │ Cache & Rate     │
  │ Requests, Audit, │                           │ Limit            │
  │ Outbox, Vectors  │                           └──────────────────┘
  └────────┬─────────┘
           │ Transactional Outbox
           ▼
  ┌──────────────────┐
  │ Apache Kafka     │
  │ Event Streaming  │
  └────────┬─────────┘
           │
           ▼
  ┌─────────────────────────────────────────────────┐
  │ Consumers: Classification, Audit, Notification  │
  └─────────────────────────────────────────────────┘
```

---

## 3. Workflow State Machine Matrix

Allowed state transitions enforced deterministically by `WorkflowStateMachine`:

| Current Status | Permitted Target Statuses |
|---|---|
| `CREATED` | `CLASSIFIED`, `ASSIGNED` |
| `CLASSIFIED` | `ASSIGNED`, `INVESTIGATING`, `ESCALATED` |
| `ASSIGNED` | `INVESTIGATING`, `ESCALATED` |
| `INVESTIGATING` | `PENDING_APPROVAL`, `ACTION_IN_PROGRESS`, `RESOLVED`, `WAITING_FOR_USER`, `ESCALATED` |
| `WAITING_FOR_USER` | `INVESTIGATING`, `RESOLVED` |
| `PENDING_APPROVAL` | `ACTION_IN_PROGRESS`, `INVESTIGATING`, `RESOLVED` |
| `ACTION_IN_PROGRESS` | `RESOLVED`, `ESCALATED`, `INVESTIGATING` |
| `ESCALATED` | `INVESTIGATING`, `ASSIGNED` |
| `RESOLVED` | `CLOSED`, `INVESTIGATING` |
| `CLOSED` | `INVESTIGATING` |

---

## 4. Controlled AI Tool Execution Model

During an AI investigation, the reasoning provider interacts exclusively via backend tool proxies in `AIToolRegistry`:

```
User Request ──► AI Agent ──► Tool Call Request (e.g. getPaymentStatus)
                                      │
                                      ▼
                           Backend Security Context
                                      │ (Verify User Permissions & RBAC)
                                      ▼
                            Execute Backend Tool
                                      │
                                      ▼
                            Return Tool Output to LLM
                                      │
                                      ▼
                       Generate Evidence-Backed Analysis
```
