<div align="center">

# InChat

A **real-time chat application** with JWT authentication, refresh tokens, user profiles, rooms, message history, read/status updates, notifications, presence events, and STOMP/WebSocket delivery.

**Stack:** Java 17 - Spring Boot 4.0.5 - MongoDB 7 - React 19 - Vite 8 - Tailwind CSS - Docker

[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MongoDB](https://img.shields.io/badge/MongoDB-7-47A248?logo=mongodb&logoColor=white)](https://www.mongodb.com/)
[![React](https://img.shields.io/badge/React-19-61DAFB?logo=react)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-8-646CFF?logo=vite)](https://vite.dev/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://docs.docker.com/compose/)
[![License](https://img.shields.io/badge/License-Educational%2Fpersonal-9B59B6)](#license)

[Backend](source/backend/in-chat) - [Frontend](source/frontend) - [Architecture](source/backend/in-chat/docs/architecture/overview.md) - [API docs](source/backend/in-chat/docs/api) - [Local setup](#local-development)

**Repository:** [github.com/trhlow/chat-application](https://github.com/trhlow/chat-application) - **Author:** Tran Hoang Long

</div>

---

## Table Of Contents

- [What Is This?](#what-is-this)
- [Core Features](#core-features)
- [Documentation](#documentation)
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
- [Contributing](#contributing)
- [License](#license)

---

<a id="what-is-this"></a>
## What Is This?

InChat is a monorepo for a realtime messaging system:

- **Backend:** Spring Boot API with MongoDB persistence, Spring Security, JWT access tokens, refresh tokens, REST APIs, and STOMP/WebSocket messaging.
- **Frontend:** React + TypeScript + Vite SPA with authentication pages, chat UI scaffolding, Tailwind CSS, shadcn-style UI components, and routing.
- **Infra:** Docker Compose for the backend service and MongoDB, plus deployment skeletons for Docker, Kubernetes, Nginx, Prometheus, and Grafana.

Current Docker Compose files under `source/backend/in-chat/infra/docker/` run the **backend + MongoDB**. The frontend is currently run separately with Vite during local development.

---

<a id="core-features"></a>
## Core Features

- **Authentication:** Register, login, refresh token, logout, logout all.
- **JWT security:** Bearer-token protected API and WebSocket connection authentication.
- **Users:** Current profile, profile update, user lookup, user search.
- **Rooms:** Create direct/group chat rooms, list current user's rooms, fetch room details.
- **Messages:** Create messages, paginate room history, update message status.
- **Realtime chat:** STOMP over WebSocket with room message topics and status topics.
- **Presence:** Online/offline lifecycle hooks through WebSocket events.
- **Notifications:** Create, list, and mark notifications as read.
- **Validation and exceptions:** Jakarta validation plus centralized error handling.
- **Tests:** Unit tests for auth and message service behavior.

---

<a id="documentation"></a>
## Documentation

| Document | Purpose |
|----------|---------|
| [source/backend/in-chat/README.md](source/backend/in-chat/README.md) | Backend-specific README |
| [source/backend/in-chat/docs/architecture/overview.md](source/backend/in-chat/docs/architecture/overview.md) | Architecture overview |
| [source/backend/in-chat/docs/architecture/modules.md](source/backend/in-chat/docs/architecture/modules.md) | Backend module map |
| [source/backend/in-chat/docs/api/authentication.md](source/backend/in-chat/docs/api/authentication.md) | Auth API notes |
| [source/backend/in-chat/docs/api/users.md](source/backend/in-chat/docs/api/users.md) | User API notes |
| [source/backend/in-chat/docs/api/rooms.md](source/backend/in-chat/docs/api/rooms.md) | Room API notes |
| [source/backend/in-chat/docs/api/messages.md](source/backend/in-chat/docs/api/messages.md) | Message API notes |
| [source/backend/in-chat/docs/deployment/local.md](source/backend/in-chat/docs/deployment/local.md) | Local deployment notes |
| [source/backend/in-chat/docs/deployment/staging.md](source/backend/in-chat/docs/deployment/staging.md) | Staging deployment notes |
| [source/backend/in-chat/docs/deployment/production.md](source/backend/in-chat/docs/deployment/production.md) | Production deployment notes |

---

<a id="project-layout"></a>
## Project Layout

```text
InChat/
  README.md
  source/
    backend/
      in-chat/
        src/main/java/com/chatrealtime/
          config/                 # Security and WebSocket configuration
          exception/              # Domain exceptions and global handler
          modules/
            auth/                 # Register, login, refresh token, logout
            user/                 # Profiles and user search
            room/                 # Chat rooms
            message/              # Message persistence and status updates
            notification/         # Notification APIs
            presence/             # Presence DTO/service
            realtime/             # STOMP message mappings
          security/               # JWT and principal services
          websocket/              # WS auth and presence listeners
        src/main/resources/       # application profiles
        src/test/java/            # unit tests
        docs/                     # architecture, API, conventions, deployment
        infra/
          docker/                 # Dockerfile and compose files
          k8s/                    # Kubernetes skeleton
          nginx/                  # Nginx skeleton
          monitoring/             # Prometheus/Grafana skeleton
        scripts/                  # dev, test, release scripts
        pom.xml
    frontend/
      src/
        pages/                    # Sign in, sign up, chat app page
        components/               # UI and auth forms
        lib/                      # shared utilities
      public/                     # static assets
      package.json
      vite.config.ts
```

---

<a id="tech-stack"></a>
## Tech Stack

| Layer | Technologies |
|-------|--------------|
| Backend | Java 17, Spring Boot 4.0.5, Spring Web MVC, Spring Security |
| Persistence | Spring Data MongoDB, MongoDB 7 |
| Auth | JWT via JJWT 0.12.6, refresh token collection |
| Realtime | Spring WebSocket, STOMP simple broker |
| Validation | Jakarta Bean Validation |
| Frontend | React 19.2, TypeScript 5.9, Vite 8, React Router 7 |
| UI | Tailwind CSS 4, shadcn-style components, Base UI, Lucide, Sonner |
| Build | Maven Wrapper, npm |
| Infra | Docker, Docker Compose, Kubernetes/Nginx/monitoring scaffolding |

---

<a id="quick-start"></a>
## Quick Start

### Backend + MongoDB With Docker

Run from the backend directory:

```bash
cd source/backend/in-chat
docker compose -f infra/docker/docker-compose.yml up --build
```

This starts:

| Service | URL |
|---------|-----|
| Backend API | http://localhost:8080 |
| MongoDB | mongodb://localhost:27017 |

Stop the stack:

```bash
docker compose -f infra/docker/docker-compose.yml down
```

### Frontend With Vite

Run from the frontend directory:

```bash
cd source/frontend
npm ci
npm run dev
```

Open http://localhost:5173.

> The current `vite.config.ts` does not define an API proxy. If the frontend starts calling backend endpoints from the browser, add a Vite proxy for `/api` and `/ws`, or configure the frontend HTTP/WebSocket clients to target `http://localhost:8080`.

---

<a id="local-development"></a>
## Local Development

### Prerequisites

- JDK 17+
- Maven 3.9+ or the included Maven Wrapper
- Node.js 24+ recommended for the current frontend dependency set
- Docker Desktop, if running MongoDB through Compose

### Option 1: Backend With Docker Compose

```bash
cd source/backend/in-chat
docker compose -f infra/docker/docker-compose.yml up --build
```

### Option 2: Backend From CLI / IDE

Start MongoDB first:

```bash
cd source/backend/in-chat
docker compose -f infra/docker/docker-compose.dev.yml up -d
```

Then run Spring Boot:

```bash
cd source/backend/in-chat
./mvnw spring-boot:run
```

PowerShell on Windows:

```powershell
cd source/backend/in-chat
.\mvnw.cmd spring-boot:run
```

### Frontend

```bash
cd source/frontend
npm ci
npm run dev
```

Build frontend:

```bash
cd source/frontend
npm run build
```

Lint frontend:

```bash
cd source/frontend
npm run lint
```

---

<a id="configuration"></a>
## Configuration

Backend configuration is loaded from `source/backend/in-chat/src/main/resources/application.yaml` and environment variables.

| Variable | Default | Purpose |
|----------|---------|---------|
| `SERVER_PORT` | `8080` | Backend HTTP port |
| `SPRING_DATA_MONGODB_URI` | `mongodb://localhost:27017/hlow_chat` | MongoDB connection string |
| `APP_JWT_SECRET` | dev fallback secret | JWT signing secret |
| `APP_JWT_ACCESS_EXPIRATION_MS` | `900000` | Access token TTL |
| `APP_JWT_REFRESH_EXPIRATION_MS` | `604800000` | Refresh token TTL |

Example:

```env
SERVER_PORT=8080
SPRING_DATA_MONGODB_URI=mongodb://localhost:27017/hlow_chat
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
| Frontend dev server | http://localhost:5173 |
| Backend API | http://localhost:8080 |
| WebSocket endpoint | ws://localhost:8080/ws |
| MongoDB | mongodb://localhost:27017 |

---

<a id="api-overview"></a>
## API Overview

Base path: `/api`

| Area | Endpoints |
|------|-----------|
| Auth | `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/refresh`, `POST /api/auth/logout`, `POST /api/auth/logout-all` |
| Users | `GET /api/users/me`, `PUT /api/users/me`, `GET /api/users`, `GET /api/users/{userId}` |
| Rooms | `POST /api/rooms`, `GET /api/rooms`, `GET /api/rooms/{roomId}` |
| Messages | `POST /api/messages`, `GET /api/messages?roomId=<id>&limit=<n>&before=<isoDateTime>`, `PATCH /api/messages/{messageId}/status` |
| Notifications | `GET /api/notifications`, `POST /api/notifications`, `PATCH /api/notifications/{notificationId}/read` |

Authentication:

```http
Authorization: Bearer <accessToken>
```

There are HTTP scratch files for quick manual testing:

- [source/backend/in-chat/src/test-auth.http](source/backend/in-chat/src/test-auth.http)
- [source/backend/in-chat/src/test-message.http](source/backend/in-chat/src/test-message.http)

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

Backend tests:

```bash
cd source/backend/in-chat
./mvnw test
```

PowerShell:

```powershell
cd source/backend/in-chat
.\mvnw.cmd test
```

Current backend unit coverage includes:

- `AuthServiceTest`
- `MessageServiceTest`
- `InChatApplicationTests`

Frontend checks:

```bash
cd source/frontend
npm run lint
npm run build
```

---

<a id="deployment-notes"></a>
## Deployment Notes

Deployment-related scaffolding exists under:

| Path | Purpose |
|------|---------|
| [source/backend/in-chat/infra/docker](source/backend/in-chat/infra/docker) | Backend Dockerfile and Compose files |
| [source/backend/in-chat/infra/k8s](source/backend/in-chat/infra/k8s) | Kubernetes notes/skeleton |
| [source/backend/in-chat/infra/nginx](source/backend/in-chat/infra/nginx) | Nginx notes/skeleton |
| [source/backend/in-chat/infra/monitoring](source/backend/in-chat/infra/monitoring) | Prometheus and Grafana skeleton |
| [source/backend/in-chat/docs/deployment](source/backend/in-chat/docs/deployment) | Local, staging, production notes |

Before production deployment:

- Replace the development JWT secret.
- Use a managed or secured MongoDB instance.
- Restrict CORS and WebSocket origins instead of using broad development defaults.
- Put the API behind TLS.
- Add a production frontend build and reverse proxy if deploying the SPA and API together.

---

<a id="contributing"></a>
## Contributing

Recommended workflow:

- Keep commits scoped to one domain.
- Use descriptive branch names such as `feature/chat-room-list` or `fix/auth-refresh-token`.
- Run backend tests before pushing backend changes.
- Run frontend lint/build before pushing frontend changes.
- Follow the backend conventions in [source/backend/in-chat/docs/conventions](source/backend/in-chat/docs/conventions).

---

<a id="license"></a>
## License

Educational / personal project. Not for commercial use without a separate agreement.

