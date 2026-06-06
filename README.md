# InChat

InChat is organized around two main areas: `frontend` and `backend`.

## Project Layout

```text
frontend/              Vite + React + TypeScript client
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
- `docker-compose.yml` runs the frontend, API service, MongoDB, and Redis locally.

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

Run the full local stack from the repository root:

```bash
docker compose up --build
```

This starts the React frontend at `http://localhost:5173`, the Spring Boot API at `http://localhost:8080`, MongoDB, and Redis.

## Main Apps

- Web: `frontend`
- API: `backend/api`
- Worker placeholder: `backend/worker`
