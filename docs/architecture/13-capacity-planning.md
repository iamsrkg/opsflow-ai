# Capacity Planning Worksheet

Do not put guessed performance figures on the resume. Use this worksheet after local load testing.

## Inputs

- average requests/sec: `R`
- peak requests/sec: `P`
- average request payload size: `B`
- average DB writes/request: `W`
- average Kafka events/request: `E`
- average AI calls/request: `A`
- average AI latency: `L_ai`

## Kafka throughput

Approximate event rate:

```text
Kafka events/sec = peak requests/sec × average events/request
```

## DB write pressure

```text
DB writes/sec = peak requests/sec × average DB writes/request
```

Include outbox, audit and idempotency records when estimating write volume.

## AI capacity

If every request synchronously calls AI:

```text
AI concurrency ≈ requests/sec × AI latency(seconds)
```

This is one reason AI workloads often deserve asynchronous isolation at higher scale.

## Scaling decision

Do not scale components just because a diagram says “microservices”. First measure:

1. CPU saturation;
2. memory pressure;
3. DB CPU/IO/locks;
4. DB connection pool;
5. Kafka lag;
6. Redis latency;
7. AI latency/cost.
