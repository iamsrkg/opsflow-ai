# Deployment Architecture

## Local

```mermaid
flowchart LR
    Browser --> FE[React dev server]
    FE --> API[Spring Boot]
    API --> PG[(PostgreSQL + pgvector)]
    API --> RD[(Redis)]
    API --> KF[(Kafka)]
```

Docker Compose starts the supporting infrastructure.

## Kubernetes target

```mermaid
flowchart TB
    IN[Ingress / Load Balancer] --> SVC[Backend Service]
    SVC --> P1[API Pod]
    SVC --> P2[API Pod]
    P1 --> PG[(Managed PostgreSQL)]
    P2 --> PG
    P1 --> RD[(Managed Redis)]
    P2 --> RD
    P1 --> KF[(Managed Kafka)]
    P2 --> KF
```

## Health checks

Use:

- liveness: process is alive;
- readiness: dependencies/initialization permit traffic;
- startup probe when initialization can be slow.

## CI/CD target

```text
Commit
 -> compile
 -> unit tests
 -> integration tests
 -> static/security scan
 -> build image
 -> image scan
 -> push registry
 -> deploy staging
 -> smoke test
 -> production approval
 -> rollout
```

## Production secrets

Never commit:

- JWT secret;
- AI API key;
- database password;
- cloud credentials.

Use a cloud secret manager/Kubernetes Secret mechanism backed by a proper secret store.
