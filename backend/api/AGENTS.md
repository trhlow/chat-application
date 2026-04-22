# AGENTS.md

## Repository Mission
This repository contains the backend for a real-time chat application built around:
- REST APIs for auth, users, friends, rooms, messages, notifications
- JWT access tokens and persisted refresh tokens
- STOMP over WebSocket for real-time delivery and presence
- MongoDB document persistence
- Avatar and message attachment upload/storage
- Spring Boot 4.x with Java 21 and virtual threads

## Mandatory Skill Loading
Before any analysis, edit, build, or verification work in this repository, Codex must load the smallest relevant skill set from `.codex/skills`.

Rules:
1. On the first non-trivial task in a session, start with `.codex/skills/chat-backend-review/SKILL.md`.
2. For any build, test, bugfix verification, or regression guard task, also load `.codex/skills/chat-test-and-regression/SKILL.md`.
3. For auth, JWT, refresh token, principal, permission, or WebSocket auth tasks, load `.codex/skills/chat-auth-jwt-refresh/SKILL.md`.
4. For STOMP, room message delivery, presence, realtime controllers, or websocket security, load `.codex/skills/chat-realtime-websocket/SKILL.md`.
5. For MongoDB document, repository, or query work, load `.codex/skills/chat-mongodb-documents/SKILL.md`.
6. For avatar, attachment, upload, or storage-provider work, load `.codex/skills/chat-media-storage-upload/SKILL.md`.
7. For endpoint design, DTO cleanup, or response contract changes, load `.codex/skills/chat-api-contracts/SKILL.md`.
8. For feature work spanning controller, service, dto, and repository, load `.codex/skills/chat-feature-slice/SKILL.md`.
9. If multiple skills apply, use the minimal set that covers the task and state the order being used.
10. Do not skip skill loading for build-related work.

## Required Inspection Order
Before structural changes, inspect these areas when relevant:
1. `README.md`
2. `pom.xml`
3. `src/main/resources/application.yml`
4. `src/main/java/com/chatrealtime/controller/*`
5. `src/main/java/com/chatrealtime/service/*`
6. `src/main/java/com/chatrealtime/repository/*`
7. `src/main/java/com/chatrealtime/security/*`
8. `src/main/java/com/chatrealtime/storage/*`
9. `src/test/java/**`

## Working Rules
1. Keep changes minimal, production-safe, and aligned with existing package boundaries under `com.chatrealtime`.
2. Preserve the current layered style: controller, dto, service/service.impl, repository, domain, security, storage, event, scheduler.
3. Prefer extending current conventions over adding parallel abstractions.
4. Preserve API compatibility unless a breaking change is explicitly requested.
5. For security-sensitive code, validate JWT handling, refresh-token lifecycle, authorization boundaries, upload validation, and WebSocket auth.
6. Add or update focused tests for meaningful behavior changes.

## Done Definition
A task is only done when:
- the relevant `.codex/skills/*/SKILL.md` files were used first
- code changes are coherent with the current backend structure
- narrow but meaningful validation has been run or explicitly described
- changed files, verification, and remaining risks are reported
