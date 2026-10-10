# Verification Checklist

## Static verification completed for this repository snapshot

- Java source tree inspected.
- Maven POM parsed and dependency declarations reviewed.
- YAML/JSON configuration inspected.
- Docker Compose configuration inspected.
- Kubernetes manifests inspected.
- Terraform starter inspected.
- CI workflow inspected.
- 61 Java source files and 64 Markdown files present in the audited archive.
- AI provider bean ambiguity fixed with explicit qualifiers.
- Request cache now implements cache-aside population on cache miss.
- Approval has optimistic locking and an explicit EXECUTED terminal state.
- Frontend defaults align with the documented development admin credentials and now exposes request creation, workflow, and approval demo actions.

## Runtime verification required on a machine with Maven/Docker

```bash
docker compose -f infra/docker-compose.yml up -d
cd backend
mvn clean test
mvn spring-boot:run
```

In another terminal:

```bash
cd frontend
npm install
npm run build
npm run dev
```

## Resume evidence

Only claim performance, availability, latency, throughput, or AI-quality numbers after running the included load/evaluation tests and recording the actual results.
