# In-Chat Backend

Production-minded Spring Boot backend for real-time chat with MongoDB persistence, JWT auth, and STOMP/WebSocket events.

## Stack

- Java 17
- Spring Boot 4.x
- Spring Web MVC
- Spring Security + JWT
- Spring Data MongoDB
- Spring WebSocket (STOMP)
- Bean Validation
- Lombok

## Run locally

### 1) Start Mongo + backend with Docker

```bash
docker compose up --build
```

### 2) Run only backend from IDE/CLI

Requirements:

- MongoDB running locally or via Docker
- `APP_JWT_SECRET` set to a strong value (at least 32 chars recommended)

```bash
./mvnw spring-boot:run
```

## Environment variables

- `SERVER_PORT` (default `8080`)
- `SPRING_DATA_MONGODB_URI` (default `mongodb://localhost:27017/hlow_chat`)
- `APP_JWT_SECRET` (default dev secret in `application.yaml`)
- `APP_JWT_EXPIRATION_MS` (default `86400000`)

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
