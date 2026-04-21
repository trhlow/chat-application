# InChat

InChat is organized around two main areas: `frontend` and `backend`.

## Project Layout

```text
frontend/
  web/                 Next.js 15 + TypeScript
  admin/               Optional React Admin app
backend/
  api/                 Spring Boot API service
  worker/              Spring Batch / background jobs
.github/
  workflows/           CI/CD workflows
```

## Root Files

- `pnpm-workspace.yaml` declares the frontend workspace packages.
- `turbo.json` defines task orchestration for frontend apps.
- `settings.gradle.kts` declares Gradle Java modules under `backend/`.
- `docker-compose.yml` runs MongoDB and the API service locally.

## Development

Install Java 21, Node.js 22, and pnpm 9.

```bash
pnpm install
pnpm dev:web
```

Run the API:

```bash
cd backend/api
./mvnw spring-boot:run
```

Run API tests:

```bash
cd backend/api
./mvnw test
```

Run local infrastructure:

```bash
docker compose up --build
```

## Main Apps

- Web: `frontend/web`
- API: `backend/api`
- Admin placeholder: `frontend/admin`
- Worker placeholder: `backend/worker`
