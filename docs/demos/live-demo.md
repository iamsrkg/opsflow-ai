# 5-Minute Live Demo

## 1. Start infrastructure
```bash
docker compose -f infra/docker-compose.yml up -d
```

## 2. Run backend
```bash
cd backend
mvn spring-boot:run
```

## 3. Login
Use a seeded admin or register an agent.

## 4. Create request
Create: `I was charged but my order is still payment pending.`

## 5. Show deterministic workflow
Show CREATED -> CLASSIFIED -> ASSIGNED -> INVESTIGATING.

## 6. Show Kafka/outbox
Query the outbox table and Kafka topic. Explain why the outbox exists.

## 7. Show Redis
Call the same GET twice; explain cache-aside and TTL. Trigger rate limiting if desired.

## 8. AI investigation
Click `Investigate with AI`. Show evidence, hypotheses, recommendations and confidence.

## 9. Human approval
Request an action, approve it as manager, then execute through the backend.

## 10. Failure demo
Stop Kafka or Redis and explain the designed failure behavior. Show that the core architecture has explicit recovery boundaries.
