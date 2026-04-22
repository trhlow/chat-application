    ---
    name: chat-auth-jwt-refresh
    description: Use for login/register/logout/refresh-token work, JWT parsing/validation, principal handling, authorization checks, security filters, websocket auth handshake, and any change touching the security package or auth controllers/services.
    ---

    # chat-auth-jwt-refresh

## Scope
Use this skill when the task touches:
- auth endpoints
- JWT access token generation/parsing/validation
- persisted refresh token lifecycle
- current user resolution
- authentication filter behavior
- WebSocket auth on CONNECT
- permission boundaries tied to authenticated users

## Relevant Packages
- `controller/AuthController.java`
- `service/AuthService.java`
- `service/RefreshTokenService.java`
- `security/*`
- `repository/RefreshTokenRepository.java`
- `domain/RefreshToken.java`
- `config/*` security-related configuration

## Working Rules
1. Keep access token and refresh token responsibilities separate.
2. Never place business logic inside filters or interceptors beyond authentication concerns.
3. Use `AuthUserPrincipal` / auth context patterns already present instead of inventing new user-resolution styles.
4. Preserve stateless request auth for HTTP endpoints.
5. Preserve WebSocket token validation flow through existing channel interception/auth mechanisms.
6. On auth changes, think through:
   - token expiry
   - refresh rotation/revocation behavior
   - logout vs logout-all semantics
   - invalid token failure path
   - compromised token blast radius
7. Avoid leaking sensitive auth failure details.
8. Keep controller DTO validation strict and service-level checks explicit.

## Validation Checklist
- register/login still work conceptually
- refresh token flow remains coherent
- logout/logout-all semantics are preserved
- protected endpoints still derive the current user correctly
- WebSocket authenticated connect path still works
- tests cover success + failure cases

## Common Pitfalls
- mixing refresh token lookup with access token trust
- trusting request user IDs instead of the authenticated principal
- breaking WebSocket auth when refactoring HTTP auth
- silently widening permitted endpoints
