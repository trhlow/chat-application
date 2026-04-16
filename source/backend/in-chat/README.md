# In-Chat Backend

Production-minded Spring Boot backend for real-time chat with JPA persistence, Flyway migrations, JWT auth, and STOMP/WebSocket events.

## Project governance

- Skill docs: [.codex/skills/](./.codex/skills) (start with [backend-skill-profile.skill.md](./.codex/skills/backend-skill-profile.skill.md))
- Rule docs: [.codex/rules/](./.codex/rules) (start with [engineering-project.rules.md](./.codex/rules/engineering-project.rules.md))
- Contribution workflow: [repository.workflow.md](./.codex/workflows/repository.workflow.md)
- Team scaffolding:
  - GitHub templates/workflows: [.github/](./.github)
  - Codex standards: [.codex/](./.codex)
  - Extended docs: [docs/](./docs)
  - Infra skeleton: [infra/](./infra)
  - Script skeleton: [scripts/](./scripts)

## Stack

- Java 21
- Spring Boot 3.x
- Spring Web MVC
- Spring Security + JWT
- Spring Data JPA
- Flyway
- PostgreSQL
- MapStruct
- Spring WebSocket (STOMP)
- Bean Validation
- Lombok

## Run locally

### 1) Start PostgreSQL + backend with Docker

```bash
docker compose -f docker/docker-compose.yml up --build
```

### 2) Run only backend from IDE/CLI

Requirements:

- PostgreSQL running locally or via Docker
- `APP_JWT_SECRET` set to a strong value (at least 32 chars recommended)

```bash
./mvnw spring-boot:run
```

## Environment variables

- `SERVER_PORT` (default `8080`)
- `SPRING_DATASOURCE_URL` (default `jdbc:postgresql://localhost:5432/in_chat`)
- `SPRING_DATASOURCE_USERNAME` (default `inchat`)
- `SPRING_DATASOURCE_PASSWORD` (default `inchat`)
- `APP_JWT_SECRET` (default dev secret in `application.yml`)
- `APP_JWT_ACCESS_EXPIRATION_MS` (default `900000`)
- `APP_JWT_REFRESH_EXPIRATION_MS` (default `604800000`)

## Auth flow

1. `POST /api/auth/register`
2. `POST /api/auth/login`
3. Use `Authorization: Bearer <accessToken>` on protected endpoints.
4. `POST /api/auth/logout` marks current user offline.

## Core REST endpoints

- Users
  - `GET /api/users/me`
  - `PUT /api/users/me`
  - `GET /api/users?query=...`
  - `GET /api/users/{userId}`
- Rooms
  - `POST /api/rooms`
  - `GET /api/rooms`
  - `GET /api/rooms/{roomId}`
- Messages
  - `POST /api/messages`
  - `GET /api/messages?roomId=<id>&limit=<n>&before=<isoDateTime>`
  - `PATCH /api/messages/{messageId}/status`
- Notifications
  - `GET /api/notifications`
  - `POST /api/notifications`
  - `PATCH /api/notifications/{notificationId}/read`

## WebSocket / STOMP

Endpoint:

- `ws://localhost:8080/ws`

Headers on `CONNECT`:

- `Authorization: Bearer <accessToken>`

Client destinations:

- Send message: `/app/rooms/{roomId}/messages`
- Update message status: `/app/messages/{messageId}/status`

Subscriptions:

- Room messages: `/topic/rooms/{roomId}/messages`
- Status updates: `/topic/rooms/{roomId}/status`
- Presence updates: `/topic/presence`

## Testing

Run tests:

```bash
./mvnw test
```

Added unit tests cover:

- Auth password/token behavior (`AuthServiceTest`)
- Message membership + status transition behavior (`MessageServiceTest`)
