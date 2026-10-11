# What happens when things fail

Each row is a behaviour of the code as it is, and where it is tested.

| What goes wrong | What the system does | Tested in |
|---|---|---|
| Kafka is down when a request changes | The API call still succeeds. The event stays `PENDING` in the outbox and is sent, in order, when Kafka is back. | `OutboxPublisherTest`, `RequestApiTest`, and `scripts/smoke.sh` in CI, which stops the Kafka container and starts it again |
| The application stops after Kafka acknowledged an event but before the row was marked published | The event is sent again on restart. The consumer recognises the id and skips it. | `KafkaFlowTest` (duplicate delivery) |
| The same event is delivered twice | It is handled once: one notification, one processed-event row. | `KafkaFlowTest` |
| A message on the topic can't be parsed | It goes straight to `<topic>.DLT`. The consumer continues with the next message. | `KafkaFlowTest` |
| A consumer fails while handling a valid event | Two retries, half a second apart, then the dead-letter topic. Its database work is rolled back each time, so nothing is half done. | error handler in `KafkaConfig` |
| An event's payload can't be serialized | The outbox row is marked `FAILED` and the rest of the batch carries on. | `OutboxPublisherTest` |
| Two people change the same request from the same stale copy | The second call gets `409` and nothing is overwritten. | `RequestApiTest` |
| Someone asks for a transition the workflow doesn't allow | `409`, whoever asks: dashboard, API client or the AI step. | `RequestApiTest`, `WorkflowStateMachineTest` |
| An agent tries to approve an action | `403`. The request stays in `PENDING_APPROVAL`. | `InvestigationFlowTest`, `scripts/smoke.sh` |
| The language model is unreachable or answers with something unreadable | The rule-based provider answers in its place. | `OpenAiCompatibleProviderTest` |
| The model recommends an action that doesn't exist | The recommendation is stored as `NEEDS_HUMAN_REVIEW`. | `AIInvestigationService`, `ActionPolicy` |
| Redis is down | Requests are not rate limited, and are otherwise unaffected. A warning is logged. | `RedisRateLimiter` |
| The embedding model is down | Search returns nothing, the investigation continues with "no runbook matched", and a warning is logged. | Handled in `VectorStoreService`; no test of its own |
| The language model is down, slow or answers with something unusable | The rules answer, and the investigation is marked as a fallback. Model calls have connect and read timeouts. | `OpenAiCompatibleProviderTest` |
| The model names an action that is not on the list | It is replaced with `NEEDS_HUMAN_REVIEW`. | Checked in `AIInvestigationService`; no test of its own |
| A client sends too many logins | `429` with `Retry-After`, before any password is checked. | `PostgresAndRedisTest`, `RateLimitFilterTest`, `scripts/smoke.sh` |
| An entity and the database schema disagree | The application refuses to start. | `PostgresAndRedisTest` (Flyway + validate) |

## Not handled

- **Two instances of the outbox publisher.** Both would send the same pending rows. Consumers would discard the duplicates, so nothing would be handled twice, but the work would be wasted. Fixing it means claiming rows with `SELECT … FOR UPDATE SKIP LOCKED`.
- **Messages on a dead-letter topic.** They stay there until someone reads them. There is no replay tool.
- **A long Kafka outage.** The outbox grows without limit. There is no alert on its size, only the count on the dashboard.
- **Token revocation.** A JWT is valid until it expires; there is no way to cancel one early.
