    ---
    name: chat-media-storage-upload
    description: Use for avatar upload, message attachment upload, storage provider abstraction, local/cloudinary storage, file-size/content-type validation, upload path handling, and storage-related configuration changes.
    ---

    # chat-media-storage-upload

## Scope
Use this skill when the task touches:
- avatar upload
- message attachment upload
- storage provider selection
- storage properties/config
- local filesystem storage
- cloudinary-related extension points
- file validation logic

## Relevant Packages
- `storage/*`
- `controller/UserController.java`
- `controller/MessageController.java`
- related DTOs and service implementations
- `application.yml` storage properties

## Working Rules
1. Validate file size and content type before storage.
2. Keep storage-provider branching inside storage layer or a narrow orchestration point.
3. Do not let controllers manage file-system logic directly.
4. Separate avatar rules from message-attachment rules.
5. Preserve or improve generated public URL behavior.
6. When adding a provider, integrate through existing property-driven selection, not ad hoc conditionals everywhere.
7. Normalize path handling and avoid dangerous filename trust.

## Security Checklist
- reject unsupported MIME types
- enforce size limits
- avoid path traversal
- sanitize filenames or generate safe names
- avoid exposing internal filesystem paths in API responses

## Verification
- avatar upload still returns the expected shape
- message attachment upload rules differ correctly by media type
- local storage directories are created/used correctly
- provider switch logic is explicit and testable
