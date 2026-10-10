# Full-stack Docker Demo

From the repository root:

```bash
docker compose -f infra/docker-compose.full.yml up --build
```

Then open `http://localhost:5173`.

Default local demo credentials are:

- username: `admin`
- password: `change-this-password-123`

For a clean reset:

```bash
docker compose -f infra/docker-compose.full.yml down -v
```

The demo image uses the deterministic AI provider by default, so no external AI key is required.
