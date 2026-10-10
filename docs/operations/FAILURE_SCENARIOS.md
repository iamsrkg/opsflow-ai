# OpsFlow AI — Failure Scenario Matrix & Resiliency Mechanisms

This document outlines the failure scenarios, root causes, and architectural mitigations embedded within OpsFlow AI.

---

### Scenario 1: DB Transaction succeeds, but Kafka publishing fails
- **Problem:** Operational request created in PostgreSQL, but broker failure drops event.
- **Architectural Solution:** Transactional Outbox Pattern. Both `Request` and `OutboxEvent` are saved within the same DB transaction. `OutboxPublisher` periodically retries unpublished events until Kafka ack is received.

---

### Scenario 2: Duplicate Kafka Event Delivery
- **Problem:** Kafka consumer receives the same `REQUEST_CREATED` message twice due to network rebalancing.
- **Architectural Solution:** Idempotent Consumer Service. Before processing, consumer checks `ProcessedEvent` table using `(eventId, consumerGroup)`. If already present, event is safely ignored.

---

### Scenario 3: LLM / AI Provider Unavailable
- **Problem:** OpenAI / LLM API endpoint returns HTTP 503 or times out.
- **Architectural Solution:** Graceful System Degradation. Standard REST CRUD, manual state transitions, assignments, and approvals continue working without interruption. Only AI classification / investigation features fallback to cached runbooks.

---

### Scenario 4: Concurrent State Updates by Multiple Operators
- **Problem:** Two operators try updating the same request simultaneously.
- **Architectural Solution:** JPA `@Version` Optimistic Locking on `Request` entity. One request succeeds; the second receives `ObjectOptimisticLockingFailureException` and is prompted to refresh state.

---

### Scenario 5: High AI Recommendation Risk (Hallucination Safeguard)
- **Problem:** AI recommends dangerous database mutation or account status reset.
- **Architectural Solution:** Human-in-the-Loop Approval Engine. Any action marked with `requiresApproval: true` generates a `PENDING` approval record requiring Manager or Admin authorization before execution.
