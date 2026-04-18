# InChat

InChat is organized as a monorepo for a realtime chat system with a web app, Spring Boot API, optional admin app, worker service, shared packages, shared Java libraries, and infra code.

## Project Layout

```text
apps/
  web/                 Next.js 15 + TypeScript
  admin/               Optional React Admin app
services/
  api/                 Spring Boot API service
  worker/              Spring Batch / background jobs
packages/
  ui/                  Shared TypeScript UI package
  types/               Shared TypeScript types
  utils/               Shared TypeScript utilities
libs/
  common-domain/       Shared Java domain library
infra/
  terraform/           Terraform IaC
  k8s/                 Kubernetes manifests
.github/
  workflows/           CI/CD workflows
```

## Root Files

- `pnpm-workspace.yaml` declares the TypeScript workspace packages.
- `turbo.json` defines monorepo task orchestration for web/packages.
- `settings.gradle.kts` declares Gradle Java modules for `libs/common-domain` and `services/worker`.
- `docker-compose.yml` runs MongoDB and the API service locally.

## Development

Install Java 21, Node.js 22, and pnpm 9.

```bash
pnpm install
pnpm dev:web
```

Run the API:

```bash
cd services/api
./mvnw spring-boot:run
```

Run API tests:

```bash
cd services/api
./mvnw test
```

Run local infrastructure:

```bash
docker compose up --build
```

## Main Apps

- Web: `apps/web`
- API: `services/api`
- Admin placeholder: `apps/admin`
- Worker placeholder: `services/worker`
