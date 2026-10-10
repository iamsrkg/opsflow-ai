# Production Gaps and Evolution Plan

This file is intentionally candid. Strong engineers know the difference between a portfolio implementation and a production platform.

| Area | Current | Production evolution |
|---|---|---|
| DB schema | Hibernate update | Flyway/Liquibase |
| Outbox publisher | polling | leased/claimed rows + multiple publishers |
| Auth | JWT access token | rotation, refresh/revocation, IdP/SSO |
| Authorization | role-centric endpoints | resource/tenant/policy authorization |
| AI embeddings | deterministic demo | evaluated production embedding model |
| AI provider | HTTP abstraction | provider routing/fallback/budget controls |
| Observability | Actuator/Micrometer/correlation | OpenTelemetry + centralized telemetry |
| Kafka DLT | configured | replay UI/process + operational ownership |
| Multi-tenancy | not implemented | tenant isolation + policy enforcement |
| Deployment | starters | managed HA infrastructure |
| DR | not implemented | backups, PITR, RPO/RTO plan |
| Performance | methodology | measured baselines and capacity model |

## Interview framing

Do not say “the project is production-ready.” Say:

> “I designed it with production concerns in mind and implemented the core reliability boundaries. I explicitly documented the remaining hardening work, such as migrations, multi-replica outbox claiming, key rotation, tracing, tenant isolation and measured capacity baselines.”
