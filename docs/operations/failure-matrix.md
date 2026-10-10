# Failure Matrix

| Failure | Immediate behavior | Correctness consequence | Recovery |
|---|---|---|---|
| PostgreSQL unavailable | API transaction fails | No authoritative state committed | Retry/backoff, restore DB |
| Redis unavailable | Cache/rate-limit path may degrade | Should not lose durable request state | Fallback policy / restore Redis |
| Kafka unavailable | Outbox remains unpublished | Request remains durable | Publisher retries |
| Kafka send succeeds, publisher crashes | Event may be resent | Duplicate delivery possible | Consumer idempotency |
| Consumer crashes | Offset not acknowledged | Redelivery | Retry |
| Poison event | Repeated failures | Partition can be affected | DLT |
| AI timeout | Investigation unavailable | Core workflow remains usable | Retry/fallback/manual investigation |
| AI bad recommendation | Wrong suggestion risk | No direct sensitive action | Human/backend controls |
| Duplicate POST | Repeated client action | Potential duplicate resource | Idempotency key |
| Concurrent update | Version conflict | Prevents silent lost update | Refresh/retry |
| Cache stale | Old read | Temporary inconsistency | TTL/invalidation |
| DLT grows | Failed processing | Operational backlog | Alert + inspect/replay |
