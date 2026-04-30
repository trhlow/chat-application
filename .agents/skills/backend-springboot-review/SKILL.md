---
name: backend-springboot-review
description: Review or improve Java Spring Boot backend code, especially controllers, services, repositories, DTOs, validation, security, transactions, and tests.
---

# Backend Spring Boot Review Skill

Use this skill when the task involves Java Spring Boot backend code.

## Review Process

1. Inspect package structure.
2. Identify controller/service/repository boundaries.
3. Check DTO usage.
4. Check validation.
5. Check authentication and authorization.
6. Check transaction boundaries.
7. Check error handling.
8. Check tests.

## Must Flag

- Entity returned directly from controller.
- Business logic inside controller.
- Missing `@Valid`.
- Missing user ownership check.
- Token issuing without strong authentication.
- Secrets in code or logs.
- Missing transaction on multi-step writes.
- Catching exceptions and hiding root cause.
- No tests for business-critical logic.

## Output Format

Return:

1. Critical issues
2. Important issues
3. Suggested patch
4. Test plan
5. Senior verdict

## Coding Rules

When editing:

- Keep changes minimal.
- Do not introduce new dependency unless necessary.
- Prefer constructor injection.
- Preserve existing style.
- Add tests for bug fixes when practical.
