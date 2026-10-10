# Scaling Strategy

## Scale path

```text
Stage 1
Single Spring Boot + PostgreSQL + Redis + Kafka

Stage 2
Multiple stateless API replicas

Stage 3
Kafka consumer parallelism + partition growth

Stage 4
Read replicas / query optimization / table partitioning

Stage 5
Extract independently scaling domains

Stage 6
Regional architecture if business requirements justify it
```

## What scales horizontally?

- Spring Boot API instances;
- Kafka consumers;
- AI worker processes;
- frontend instances.

## What needs careful coordination?

- PostgreSQL writes;
- outbox publishing;
- Redis consistency;
- workflow concurrency;
- Kafka partition count/order;
- approval execution.

## 10x API traffic

1. Measure current bottleneck.
2. Add stateless API replicas.
3. Put a load balancer in front.
4. Tune DB pool and queries.
5. Cache hot reads.
6. Move expensive AI work asynchronous where user experience allows.
7. Scale Kafka partitions/consumers.
8. Monitor lag and DB saturation.

## When to extract services

Extract only when one or more is true:

- independent scaling requirement;
- independent deployment cadence;
- clear team ownership;
- fault isolation requirement;
- domain boundary is stable.

A reasonable first extraction could be AI processing because it has different latency/cost/scaling characteristics from CRUD request management.
