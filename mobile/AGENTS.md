# mobile/AGENTS.md

## Mobile Scope

This directory contains the Flutter / Dart mobile application.

## Flutter Architecture Rules

- Separate UI from business logic.
- Keep API calls outside widgets.
- Keep models clean and typed.
- Keep reusable widgets small.
- Avoid large stateful widgets that contain API, validation, and rendering logic together.

Recommended structure:

- lib/core
- lib/features
- lib/shared
- lib/data
- lib/domain if the app grows
- lib/routes
- lib/theme

## Dart Rules

- Use null safety correctly.
- Avoid `dynamic` unless necessary.
- Prefer immutable models where possible.
- Use meaningful names.
- Avoid duplicated UI logic.
- Do not silence analyzer warnings without reason.

## API Rules

- Handle timeout, network failure, bad response, unauthorized, and server error.
- Do not crash when backend returns unexpected data.
- Token refresh/logout behavior must be explicit.
- Do not log tokens or sensitive payloads.

## UI/UX Rules

Every screen should handle:

- Loading state
- Empty state
- Error state
- Success state

For forms:

- Validate required fields
- Show clear error messages
- Prevent duplicate submission
- Handle keyboard overflow
- Use proper input types

## Android Rules

- If the app calls APIs, ensure release/profile Android manifest has INTERNET permission.
- If using cleartext HTTP for local development, configure it intentionally and do not treat it as production-safe.
- Check release behavior, not only debug behavior.

## Testing Rules

For mobile changes, run or recommend:

- `flutter analyze`
- `flutter test`
- Manual check on emulator/device
- Release config check when API/networking is touched
