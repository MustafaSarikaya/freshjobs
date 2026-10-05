# Fresh Jobs

Job scouting application: a Spring Boot backend with PostgreSQL and a React SPA. Monitors company job boards via public APIs (Greenhouse, Lever, Ashby, Workday, BambooHR, Phenom, Jibe) and tracks new/expired listings.

## Local Development

```bash
docker compose up --build
```

The app runs at http://localhost:8080. Health check: http://localhost:8080/api/v1/health

For frontend work with hot reload, run `npm run dev` in `frontend/`.

### Running Tests

```bash
cd backend && mvn test
cd frontend && npm run test
```

### Access

The public site has no sign-in. Saved views and "seen" job markers are kept in the visitor's HTTP session only and are lost when the session expires.

The admin area lives at `/admin` (sign in at `/admin/login`). Only staff and superuser accounts can sign in. It manages sources, location tags, and ingestion runs.

### API

The contract is `openapi/openapi.yaml`, served under `/api/v1`. Swagger UI is available at `/swagger-ui.html`.

| Area | Auth |
|------|------|
| Health, jobs, sources (read), locations (read), saved views | None |
| `/api/v1/runs` (read) and `/api/v1/admin/**` | Admin session |
| `POST /api/v1/runs` | Admin session or `INGEST_API_KEY` bearer token |

## Advanced Filter Expression Contract

`GET /api/jobs/` accepts the optional `filter` query parameter containing JSON for a canonical filter expression AST.

Shape:

- Group nodes:
  - `{ "op": "and", "children": [<node>, ...] }`
  - `{ "op": "or", "children": [<node>, ...] }`
  - `{ "op": "not", "child": <node> }`
- Predicate nodes:
  - `{ "field": "title", "operator": "contains", "value": "engineer" }`

Example:

```json
{
  "op": "and",
  "children": [
    { "field": "title", "operator": "contains", "value": "engineer" },
    {
      "op": "not",
      "child": { "field": "title", "operator": "contains", "value": "senior" }
    }
  ]
}
```

Compatibility notes:

- Existing quick filters (`source_id`, `status`) remain supported.
- Effective query semantics are `quick_filters AND advanced_filter` when both are present.
- The AST format is intentionally deterministic so it can be reused by future notifications without changing filter meaning.

## Deployment

Deployed to K3s homeserver at `jobs.halitdincer.com` via ArgoCD.

### Deployment Flow

1. Code merges to `main`
2. GitHub Actions (`ci.yml`) builds the Docker image and pushes to `ghcr.io/halitdincer/job-scout` with `latest` and SHA tags
3. ArgoCD Image Updater detects the new image digest
4. ArgoCD auto-syncs and redeploys the job-scout pods on K3s

### Scheduled Ingestion

GitHub Actions (`ingest.yml`) runs every 4 hours via cron, calling `POST /api/runs/` with the `INGEST_API_KEY` secret. Can also be triggered manually from the Actions tab.

### Required Vault Secrets

Set these at `secret/job-scout/config` in Vault:

| Key | Description |
|-----|-------------|
| `DATABASE_URL` | PostgreSQL connection string: `postgres://postgres:<password>@postgres:5432/jobscout` |
| `POSTGRES_PASSWORD` | PostgreSQL password (must match the password in DATABASE_URL) |
| `INGEST_API_KEY` | API key for triggering ingestion (also set as GitHub repo secret `INGEST_API_KEY`) |
