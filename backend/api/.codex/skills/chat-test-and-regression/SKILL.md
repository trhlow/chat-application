    ---
    name: chat-test-and-regression
    description: Use for writing or updating backend tests, validating bug fixes, preventing regressions, and deciding the narrowest meaningful verification for controllers, services, auth logic, repositories, or storage behavior.
    ---

    # chat-test-and-regression

## Scope
Use when the task is about:
- adding tests
- reproducing a bug
- guarding against regressions
- validating a refactor
- deciding what to run after a change

## Test Strategy
Prefer this order:
1. unit tests for service/security/storage logic
2. focused slice/context tests only where wiring matters
3. full integration only if the task truly needs it

## Relevant Existing Areas
Look for tests under:
- `src/test/java/com/chatrealtime/**`
- especially auth, message, user, friend, and application context tests

## Rules
1. Test behavior, not implementation trivia.
2. When fixing a bug, write the failing test first if practical.
3. Cover happy path and the most important failure path.
4. Keep fixtures readable and domain-relevant.
5. Do not over-mock when the behavior under test is mainly mapping or validation.
6. Prefer narrow verification commands where possible.

## Verification Reporting
Always state:
- what tests were added/updated
- what command was run, or what should be run if execution is unavailable
- what remains unverified

## Common Bug Areas In This Repo
- JWT/refresh edge cases
- room membership assumptions
- message status transitions
- friend request state transitions
- upload validation
- presence event side effects
