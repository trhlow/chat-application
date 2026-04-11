# Engineering Rules

These rules are mandatory for this repository.

1. Keep package organization under `com.chatrealtime.*`.
2. Keep layered boundaries: controller -> service -> repository.
3. Keep controllers focused on transport concerns only.
4. Use DTOs for request/response boundaries.
5. Do not return password or internal security fields in API responses.
6. Do not trust client-provided user identity for protected actions.
7. Require authenticated principal for private endpoints.
8. Enforce room membership checks for message read/write/status actions.
9. Keep message status transitions monotonic (`sent -> delivered -> read`).
10. Use validation annotations for request DTOs.
11. Keep global error response shape consistent.
12. Add/adjust tests for non-trivial service logic changes.
13. Avoid unnecessary libraries and avoid speculative abstractions.
14. Do not hardcode secrets; use environment variables/config.
