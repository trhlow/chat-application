---
name: flutter-mobile-review
description: Review or improve Flutter/Dart mobile code, especially screens, API calls, state handling, Android manifest, release networking, validation, and UX states.
---

# Flutter Mobile Review Skill

Use this skill when the task involves Flutter or Dart mobile app code.

## Review Process

1. Inspect feature structure.
2. Check widget responsibility.
3. Check API client usage.
4. Check loading/error/empty/success states.
5. Check form validation.
6. Check token and local storage handling.
7. Check Android permissions and release behavior.
8. Check analyzer/test readiness.

## Must Flag

- API calls directly inside large widgets.
- Missing loading/error state.
- Missing Android INTERNET permission in main manifest.
- Debug-only networking setup used as if production-safe.
- Plain logging of sensitive data.
- Unsafe token storage.
- Duplicate submit bugs.
- Hardcoded dimensions that break small screens.

## Output Format

Return:

1. UI/state issues
2. API/networking issues
3. Android/release issues
4. Suggested patch
5. Verification commands

## Verification

Recommend:

- `flutter analyze`
- `flutter test`
- manual emulator check
- release/profile config check
