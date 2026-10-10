# Observability

## Three pillars

### Metrics

Examples:

- request creation count;
- API latency;
- error count;
- Kafka lag;
- consumer retry count;
- AI latency;
- cache hit/miss ratio;
- rate-limit rejections.

### Logs

Use structured logs with request/event/correlation identifiers.

### Traces

The repository establishes correlation IDs. Full OpenTelemetry export is a production evolution item.

## Correlation flow

```text
HTTP X-Correlation-Id
        |
        v
Request correlationId
        |
        v
Outbox event correlationId
        |
        v
Kafka consumer logs
        |
        v
Downstream investigation / troubleshooting
```

## Why correlation ID matters

When an operator reports that a request failed, the engineer should be able to follow one identifier across HTTP logs, domain records and Kafka events.

## SLO examples

These are design examples, **not measured claims**:

- API availability: 99.9%
- p95 CRUD latency: < 300 ms under a defined load
- event processing delay: < 5 seconds for normal load
- AI investigation success rate: > 99% transport availability

Only publish actual values after load testing.
