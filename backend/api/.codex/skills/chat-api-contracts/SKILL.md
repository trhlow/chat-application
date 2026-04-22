    ---
    name: chat-api-contracts
    description: Use for endpoint design, DTO cleanup, response consistency, validation rules, exception mapping, Swagger/OpenAPI accuracy, and any task that changes the public REST API or request/response semantics.
    ---

    # chat-api-contracts

## Scope
Use this skill for:
- request/response DTO changes
- endpoint path/method design
- validation annotations
- error response consistency
- OpenAPI/Swagger-visible behavior

## Relevant Packages
- `controller/*`
- `dto/request/*`
- `dto/response/*`
- `exception/*`
- OpenAPI config under `config/`

## Working Rules
1. Keep API contracts explicit and predictable.
2. Use DTOs instead of exposing domain documents directly.
3. Validate input as early as is sensible.
4. Preserve naming consistency across auth, users, friends, rooms, messages, notifications.
5. Prefer additive API changes over breaking ones.
6. If making a breaking change, document it clearly and update affected callers/tests/docs.

## Contract Checklist
- Is the endpoint name consistent with neighboring endpoints?
- Is the HTTP method semantically right?
- Are validation errors surfaced consistently?
- Is the response minimal but sufficient?
- Is auth required and documented correctly?
- Does Swagger/OpenAPI still reflect reality?

## Avoid
- leaking internal IDs/fields accidentally
- inconsistent pagination parameter patterns
- response wrappers that do not match repo conventions
