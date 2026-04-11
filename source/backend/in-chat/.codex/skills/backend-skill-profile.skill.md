# Backend Skill Profile

This backend expects contributors to work with the following skill baseline.

## Core skills

- Spring Boot API design (controller/service/repository separation)
- Spring Security (JWT, route protection, authenticated principal)
- MongoDB modeling and query performance basics
- STOMP/WebSocket event flows for real-time chat
- Bean Validation and stable API contracts
- Unit testing with JUnit + Mockito

## Domain skills

- Chat room membership authorization
- Message persistence and history pagination
- Delivery/read receipt transitions
- Presence state consistency (REST + WebSocket)
- Error handling and API failure contracts

## Engineering expectations

- Keep controllers thin and business logic in services.
- Do not expose persistence entities directly from APIs.
- Never store or compare plaintext passwords.
- Validate all external input.
- Keep changes incremental and aligned with existing package style.
