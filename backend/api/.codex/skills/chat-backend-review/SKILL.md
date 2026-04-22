    ---
    name: chat-backend-review
    description: Use when asked to analyze, audit, plan, review, or understand this chat backend repository before coding. Applies to architecture reviews, sprint planning, impact analysis, and identifying how auth, messages, rooms, notifications, storage, repositories, and websocket pieces fit together.
    ---

    # chat-backend-review

## Purpose
Understand the backend before changing it. This skill is the starting point for audits, planning, architectural review, repo familiarization, and “what should I do next?” tasks.

## Read First
Inspect, in this order when relevant:
1. `README.md`
2. `pom.xml`
3. `src/main/resources/application.yml`
4. `src/main/java/com/chatrealtime/controller/*`
5. `src/main/java/com/chatrealtime/service/*`
6. `src/main/java/com/chatrealtime/repository/*`
7. `src/main/java/com/chatrealtime/security/*`
8. `src/main/java/com/chatrealtime/storage/*`
9. `src/main/java/com/chatrealtime/event/*`
10. `src/test/java/**`

## What To Extract
Summarize:
- main bounded areas: auth, users, friends, rooms, messages, notifications, presence, storage
- request flow from controller -> service -> repository/storage
- security flow for HTTP and WebSocket
- persistence model in MongoDB documents
- known extension points
- likely technical debt and missing tests

## Review Lens
Evaluate with this order of importance:
1. correctness
2. security
3. cohesion with current structure
4. maintainability
5. performance
6. developer ergonomics

## Output Format
Return:
- current architecture snapshot
- strengths
- risks / code smells
- recommended next tasks in priority order
- concrete file paths likely to be changed for each recommendation

## Avoid
- proposing a rewrite by default
- introducing layers that duplicate existing ones
- making claims without pointing to concrete packages/files
