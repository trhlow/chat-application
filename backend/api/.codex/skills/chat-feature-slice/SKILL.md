    ---
    name: chat-feature-slice
    description: Use when implementing or extending a backend feature end-to-end across controller, DTO, service, repository, domain, and tests while staying within the current chat-application structure.
    ---

    # chat-feature-slice

## Purpose
Implement feature work as a vertical slice that respects the repository’s current layering.

## Standard Flow
For most feature work:
1. identify the public contract
2. define/update request/response DTOs
3. update controller mapping minimally
4. implement service logic
5. extend repository/domain only if necessary
6. add focused tests
7. update docs only if behavior changed externally

## File Placement Rules
- transport/API concerns -> `controller`, `dto`
- business orchestration -> `service`, `service/impl`
- persistence details -> `repository`, `domain`
- authentication/identity -> `security`
- uploads -> `storage`
- lifecycle/background effects -> `event`, `scheduler`

## Constraints
1. Keep controllers thin.
2. Prefer explicit service methods over generic “doEverything” methods.
3. Avoid putting mapping or validation in too many layers at once.
4. Reuse existing naming conventions for endpoints, DTOs, and services.
5. Preserve consistent API error behavior with global exception handling.

## Change Template
For each feature, think:
- who calls it?
- what is the API contract?
- who is authorized?
- what data changes?
- what realtime side effects happen?
- what notifications or presence side effects happen?
- what tests prove it?

## Avoid
- cross-cutting edits without a clear need
- mixing message, room, friend, and notification logic carelessly
- skipping tests on non-trivial service logic
