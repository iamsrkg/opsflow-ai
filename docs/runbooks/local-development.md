# Local Development Runbook

Prerequisites: JDK 21+, Maven 3.9+, Docker Desktop, Node.js 20+.

```bash
docker compose -f infra/docker-compose.yml up -d
cd backend
mvn test
mvn spring-boot:run
```

Frontend:
```bash
cd frontend
npm install
npm run dev
```

Set production-like environment variables rather than using defaults when sharing screenshots or deploying publicly.
