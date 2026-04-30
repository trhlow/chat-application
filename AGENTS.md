# AGENTS.md

## Project Overview

This is a personal mobile application project with:

- Backend: Java, Spring Boot
- Mobile: Flutter / Dart
- Database: PostgreSQL or relational database depending on environment
- Purpose: personal taxi income/order management, authentication, data privacy, calculation history, PDF/file-based input when applicable

The agent must behave like a senior full-stack reviewer, not a blind code generator.

## Global Working Rules

- Never rewrite the whole project unless explicitly requested.
- Before editing, inspect the existing structure, naming, dependencies, and current implementation.
- Prefer small, safe, reviewable changes.
- Do not invent APIs, tables, DTOs, routes, environment variables, or business rules.
- If a requirement is unclear, infer conservatively from the existing code and document the assumption.
- Do not remove working code unless it is unsafe, duplicated, dead, or clearly wrong.
- Do not add new dependencies unless there is a strong reason.
- Always preserve the current architecture unless there is a clear defect.

## Security Rules

- Never hardcode secrets, API keys, passwords, tokens, or private URLs.
- Never expose access tokens in logs.
- Never create unauthenticated endpoints that issue user tokens.
- Any endpoint that creates, restores, exports, or reads private user data must require authentication.
- Validate all request input.
- Prefer DTOs over exposing entities directly.
- Use explicit authorization checks for user-owned data.
- Mobile apps must not rely on hidden client-side secrets for security.

## Backend Rules

- Use clear package boundaries:
  - controller
  - service
  - repository
  - dto
  - entity/model
  - config
  - security
  - exception
- Controllers should be thin.
- Business logic belongs in services.
- Persistence logic belongs in repositories.
- Do not return JPA entities directly from controllers.
- Use meaningful exceptions and centralized error handling.
- Use transactions for write operations that touch multiple records.
- Add tests for important business logic and security-sensitive changes.

## Mobile Rules

- Keep UI, state, API client, models, and storage separated.
- Do not put business logic directly inside widgets when it grows beyond simple UI state.
- Handle loading, empty, error, and success states.
- API failures must show useful messages.
- Local storage must not store sensitive tokens insecurely if a safer option exists.
- Android release builds must include required permissions such as INTERNET when API calls are used.

## Testing Rules

Before considering a task complete:

- Backend:
  - Run unit tests if available.
  - Run integration tests if the change touches database/security/API behavior.
  - Check authentication and authorization flows.
- Mobile:
  - Run flutter analyze.
  - Run tests if available.
  - Check Android release-specific configuration when networking is involved.

If commands cannot be run, state that clearly and explain what should be run manually.

## Output Format

When reviewing code, respond with:

1. Critical issues
2. Important improvements
3. Nice-to-have improvements
4. Files changed or suggested
5. Tests to run
6. Final senior verdict

When implementing code, respond with:

1. What changed
2. Why it changed
3. Files affected
4. Risks
5. Commands to verify

## Definition of Done

A change is done only when:

- It compiles
- It does not break existing behavior
- It has validation where needed
- It has error handling
- It has tests or a clear manual verification path
- Security impact has been considered
