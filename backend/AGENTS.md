# backend/AGENTS.md

## Backend Scope

This directory contains the Java Spring Boot backend.

## Backend Architecture Rules

- Follow layered architecture:
  - Controller: HTTP request/response only
  - Service: business rules
  - Repository: database access
  - DTO: API input/output
  - Entity: persistence model
  - Mapper: DTO/entity conversion if needed
  - Security: filters, JWT, authentication, authorization
  - Exception: global error handling

## Java Rules

- Prefer Java 21+ compatible code unless the project config says otherwise.
- Use constructor injection.
- Avoid field injection.
- Avoid static mutable state.
- Avoid magic numbers and magic strings.
- Use records for simple immutable DTOs when appropriate.
- Use clear method names that describe business intent.

## Spring Boot Rules

- Do not put business logic in controllers.
- Do not expose entities directly through REST APIs.
- Use `@Valid` for request DTO validation.
- Use `@Transactional` on service methods that modify data.
- Use centralized exception handling with `@RestControllerAdvice`.
- Keep security configuration explicit and minimal.
- Never disable CSRF/CORS/security globally without explaining why.

## API Rules

- REST endpoints should use consistent naming.
- Return proper HTTP status codes.
- Do not return stack traces to clients.
- Do not leak internal IDs if the domain does not require them.
- Always check that authenticated users can only access their own data.

## Database Rules

- Do not change schema casually.
- For schema changes, prefer migration scripts if the project uses Flyway/Liquibase.
- Avoid destructive migrations unless explicitly requested.
- Add indexes for frequently queried fields when justified.

## Testing Rules

For backend changes, prefer:

- Unit tests for services
- Controller tests for API validation and status codes
- Integration tests for repository/security/database behavior
- Regression tests for bug fixes

## Review Priority

Always check these first:

1. Authentication bypass
2. Token issuing endpoints
3. User ownership checks
4. Input validation
5. Data privacy leaks
6. Broken transaction boundaries
7. Incorrect money/date calculation
8. Missing tests
