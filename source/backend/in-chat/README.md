<div align="center">

# InChat Backend

A **real-time chat backend** with JWT authentication, refresh tokens, user profiles, rooms, message history, message attachments, friend requests, notifications, presence events, and STOMP/WebSocket delivery.

**Stack:** Java 21 - Spring Boot 4.0.5 - PostgreSQL - Spring Data JPA - Flyway - Docker

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Flyway](https://img.shields.io/badge/Flyway-Migrations-CC0200?logo=flyway&logoColor=white)](https://flywaydb.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://docs.docker.com/compose/)
[![License](https://img.shields.io/badge/License-Educational%2Fpersonal-9B59B6)](#license)

[Local Setup](#local-development) - [API Overview](#api-overview) - [WebSocket](#websocket--stomp) - [Testing](#testing)

**Repository:** [github.com/trhlow/chat-application](https://github.com/trhlow/chat-application) - **Author:** Tran Hoang Long

</div>

---

## Table Of Contents

- [What Is This?](#what-is-this)
- [Core Features](#core-features)
- [Project Layout](#project-layout)
- [Tech Stack](#tech-stack)
- [Quick Start](#quick-start)
- [Local Development](#local-development)
- [Configuration](#configuration)
- [Access URLs](#access-urls)
- [API Overview](#api-overview)
- [WebSocket / STOMP](#websocket--stomp)
- [Testing](#testing)
- [Deployment Notes](#deployment-notes)
- [License](#license)

---

<a id="what-is-this"></a>
## What Is This?

InChat Backend is the Spring Boot service for a realtime chat system:

- **REST API:** Authentication, users, rooms, messages, friends, notifications, and profile media.
- **Realtime delivery:** STOMP over WebSocket for room messages, message status updates, and presence events.
- **Persistence:** PostgreSQL with Spring Data JPA and Flyway-managed schema migrations.
- **Security:** Spring Security with JWT access tokens and persisted refresh tokens.
- **Storage:** Local or Cloudinary-backed avatar and message attachment uploads.

---

<a id="core-features"></a>
## Core Features

- **Authentication:** Register, login, refresh token, logout, logout all.
- **JWT security:** Bearer-token protected APIs and WebSocket connection authentication.
- **Users:** Current profile, profile update, avatar upload, user lookup, user search.
- **Friends:** Send, accept, reject, cancel friend requests, list friends, remove friends.
- **Rooms:** Create direct/group rooms, list current user's rooms, fetch room details.
- **Messages:** Create text messages, upload message attachments, paginate history, update read/delivery status.
- **Realtime chat:** STOMP room message topics and message status topics.
- **Presence:** Online/offline lifecycle hooks through WebSocket events.
- **Notifications:** Create, list, system notifications, and mark notifications as read.
- **Observability:** Actuator, metrics, tracing configuration, and OpenAPI/Swagger UI.
- **Validation and exceptions:** Jakarta validation plus centralized JSON error handling.
- **Tests:** Unit and context tests for core service behavior.

---

<a id="project-layout"></a>
## Project Layout

```text
in-chat/
  docker/
    Dockerfile
    docker-compose.yml
  src/main/java/com/chatrealtime/
    client/                 # HTTP service clients
    config/                 # Security, WebSocket, MVC, cache, OpenAPI, async config
    constant/               # Shared constants
    controller/             # REST and realtime controllers
    domain/                 # JPA entities
    dto/
      request/              # Request contracts
      response/             # Response contracts
    event/                  # Application and WebSocket event listeners
    exception/              # Domain exceptions and global handler
    mapper/                 # DTO mappers
    observability/          # Tracing and metrics configuration
    repository/             # Spring Data JPA repositories
    scheduler/              # Scheduled maintenance jobs
    security/               # JWT, principal, and WebSocket auth
    service/                # Service interfaces
    service/impl/           # Service implementations
    storage/                # Avatar and attachment storage
    util/                   # Utility helpers
  src/main/resources/
    db/migration/           # Flyway migrations
    i18n/                   # Messages
    application.yml
    application-*.yml
  src/test/java/com/chatrealtime/
    unit/                   # Unit tests
  pom.xml
```

---

<a id="tech-stack"></a>
## Tech Stack

| Layer | Technologies |
|-------|--------------|
| Runtime | Java 21, Spring Boot 4.0.5 |
| API | Spring Web MVC, Jakarta Bean Validation, OpenAPI/Swagger |
| Security | Spring Security, JJWT 0.12.6, refresh token persistence |
| Persistence | Spring Data JPA, PostgreSQL, Flyway |
| Realtime | Spring WebSocket, STOMP simple broker |
| Mapping | MapStruct and local mapper components |
| Storage | Local filesystem, Cloudinary integration hooks |
| Observability | Spring Actuator, Micrometer/OpenTelemetry configuration |
| Build/Test | Maven, JUnit, Mockito, H2 test profile |
| Infra | Docker, Docker Compose |

---

<a id="quick-start"></a>
## Quick Start

### Backend + PostgreSQL With Docker

Run from this backend directory:

```bash
docker compose -f docker/docker-compose.yml up --build
```

This starts:

| Service | URL |
|---------|-----|
| Backend API | http://localhost:8080 |
| PostgreSQL | jdbc:postgresql://localhost:5432/in_chat |

Stop the stack:

```bash
docker compose -f docker/docker-compose.yml down
```

---

<a id="local-development"></a>
## Local Development

### Prerequisites

- JDK 21+
- Maven 3.9+ or the included Maven Wrapper
- Docker Desktop, if running PostgreSQL through Compose

### Run From CLI / IDE

Start PostgreSQL first:

```bash
docker compose -f docker/docker-compose.yml up -d postgres
```

Run Spring Boot:

```bash
./mvnw spring-boot:run
```

PowerShell on Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

---

<a id="configuration"></a>
## Configuration

Backend configuration is loaded from `src/main/resources/application.yml`, profile-specific `application-*.yml` files, and environment variables.

| Variable | Default | Purpose |
|----------|---------|---------|
| `SERVER_PORT` | `8080` | Backend HTTP port |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/in_chat` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `inchat` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | `inchat` | Database password |
| `APP_JWT_SECRET` | dev fallback secret | JWT signing secret |
| `APP_JWT_ACCESS_EXPIRATION_MS` | `900000` | Access token TTL |
| `APP_JWT_REFRESH_EXPIRATION_MS` | `604800000` | Refresh token TTL |
| `APP_STORAGE_PROVIDER` | `local` | Avatar/attachment storage provider |
| `APP_STORAGE_LOCAL_UPLOAD_DIR` | `uploads/avatars` | Local avatar upload directory |
| `APP_MESSAGE_ATTACHMENT_LOCAL_UPLOAD_DIR` | `uploads/message-attachments` | Local attachment upload directory |

Example:

```env
SERVER_PORT=8080
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/in_chat
SPRING_DATASOURCE_USERNAME=inchat
SPRING_DATASOURCE_PASSWORD=inchat
APP_JWT_SECRET=change-this-to-a-strong-secret-with-at-least-32-chars
APP_JWT_ACCESS_EXPIRATION_MS=900000
APP_JWT_REFRESH_EXPIRATION_MS=604800000
```

Do not commit production secrets. Use environment variables or deployment secret management.

---

<a id="access-urls"></a>
## Access URLs

| Service | Local URL |
|---------|-----------|
| Backend API | http://localhost:8080 |
| WebSocket endpoint | ws://localhost:8080/ws |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Actuator health | http://localhost:8080/actuator/health |
| PostgreSQL | jdbc:postgresql://localhost:5432/in_chat |

---

<a id="api-overview"></a>
## API Overview

Base path: `/api`

| Area | Endpoints |
|------|-----------|
| Auth | `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/refresh`, `POST /api/auth/logout`, `POST /api/auth/logout-all` |
| Users | `GET /api/users/me`, `PUT /api/users/me`, `POST /api/users/me/avatar`, `GET /api/users`, `GET /api/users/{userId}` |
| Friends | `POST /api/friends/requests`, `GET /api/friends/requests/incoming`, `GET /api/friends/requests/outgoing`, `POST /api/friends/requests/{id}/accept`, `POST /api/friends/requests/{id}/reject`, `DELETE /api/friends/requests/{id}`, `GET /api/friends`, `DELETE /api/friends/{friendId}` |
| Rooms | `POST /api/rooms`, `GET /api/rooms`, `GET /api/rooms/{roomId}` |
| Messages | `POST /api/messages`, `POST /api/messages/attachments`, `GET /api/messages?roomId=<id>&limit=<n>&before=<isoDateTime>`, `PATCH /api/messages/{messageId}/status` |
| Notifications | `GET /api/notifications`, `POST /api/notifications`, `PATCH /api/notifications/{notificationId}/read` |

Authentication:

```http
Authorization: Bearer <accessToken>
```

---

<a id="websocket--stomp"></a>
## WebSocket / STOMP

Endpoint:

```text
ws://localhost:8080/ws
```

The client must send the JWT on `CONNECT`:

```text
Authorization: Bearer <accessToken>
```

Client destinations:

| Purpose | Destination |
|---------|-------------|
| Send room message | `/app/rooms/{roomId}/messages` |
| Update message status | `/app/messages/{messageId}/status` |

Subscriptions:

| Purpose | Topic |
|---------|-------|
| Room messages | `/topic/rooms/{roomId}/messages` |
| Room status updates | `/topic/rooms/{roomId}/status` |
| Presence updates | `/topic/presence` |

---

<a id="testing"></a>
## Testing

Run backend tests:

```bash
./mvnw test
```

PowerShell:

```powershell
.\mvnw.cmd test
```

Current backend coverage includes:

- `InChatApplicationTests`
- `AuthServiceTest`
- `MessageServiceTest`
- `UserServiceTest`
- `FriendServiceTest`

---

<a id="deployment-notes"></a>
## Deployment Notes

Before production deployment:

- Replace the development JWT secret.
- Use a managed or secured PostgreSQL instance.
- Restrict CORS and WebSocket origins.
- Put the API behind TLS.
- Configure object storage credentials when using Cloudinary.
- Keep Flyway migrations immutable after release.

---

<a id="license"></a>
## License

Educational / personal project. Not for commercial use without a separate agreement.
