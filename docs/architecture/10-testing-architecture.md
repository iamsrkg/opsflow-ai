# Testing Architecture

## Test pyramid

```text
             E2E
           /-----\\
       Integration
      /-----------\\
          Unit
     /---------------\\
```

## Unit tests

Fast tests for:

- workflow transitions;
- validation rules;
- classification thresholds;
- deterministic AI provider;
- rate-limit decisions.

## Integration tests

Use real infrastructure containers where practical:

- PostgreSQL;
- Kafka;
- Redis.

## End-to-end test

The flagship scenario is:

```text
login
 -> create request
 -> verify outbox
 -> publish event
 -> consume event
 -> classify
 -> retrieve knowledge
 -> investigate
 -> request approval
 -> approve
 -> execute
 -> verify audit/history
```

## Performance test

Use k6. Record:

- RPS;
- p50/p95/p99 latency;
- errors;
- CPU;
- memory;
- DB connection utilization;
- Kafka lag.

Never put unmeasured numbers in the README or resume.

## AI evaluation

Use a versioned synthetic dataset and record:

- classification accuracy;
- retrieval relevance;
- unsupported-claim rate;
- structured output validity;
- latency;
- token cost.
