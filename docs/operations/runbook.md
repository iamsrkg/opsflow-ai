# Operations Runbook

## API unhealthy

1. Check `/actuator/health`.
2. Check logs with correlation ID.
3. Check PostgreSQL connectivity.
4. Check Redis.
5. Check Kafka broker/consumer health.

## Kafka lag rising

1. Inspect consumer lag.
2. Identify slow handler.
3. Check downstream dependency latency.
4. Scale consumers if partition count allows.
5. Consider pausing expensive synchronous AI work.

## Outbox backlog rising

1. Check Kafka connectivity.
2. Check publisher errors.
3. Inspect oldest unpublished row.
4. Confirm database capacity.
5. Restore Kafka/publisher and monitor drain rate.

## DLT growth

1. Identify event type and exception.
2. Determine code/data/config cause.
3. Fix cause.
4. Replay through controlled tooling.
5. Confirm deduplication and audit.

## Redis failure

1. Confirm Redis health.
2. Ensure PostgreSQL fallback remains available.
3. Decide rate-limit fail-open/fail-closed according to endpoint risk.
4. Restore Redis.
5. Monitor cache warm-up.

## AI provider failure

1. Check timeout/error rate.
2. Confirm core request workflow still works.
3. Switch provider/fallback if supported.
4. Avoid retry storms.
5. Track AI-specific SLO separately from core API SLO.
