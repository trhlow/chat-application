# Backend ZaloChat Implementation Plan

## Implemented in this pass

- Refresh token cookie support with HttpOnly/Secure/SameSite while preserving JSON token compatibility.
- `clientMessageId` idempotency for REST/STOMP message creation and attachment message creation.
- User block model/API plus friend/direct-message enforcement.
- Group owner transfer, admin promote/demote, member list, embedded group settings.
- Group send permission enforcement for `ADMIN_ONLY`.
- Group join request approval flow: member creates pending request, admin approves/rejects.
- Mongo index scripts for user blocks and group join requests.
- Regression tests for block, idempotency, group send permission, owner transfer, and invite request.

## MVP bat buoc lam ngay

1. Auth/session hardening
   - Done: Add refresh token cookie support with `HttpOnly`, `Secure`, `SameSite`.
   - Preserve existing JSON refresh token fields for backward compatibility until frontend migrates.
   - Allow refresh/logout to read refresh token from cookie when request body is absent.

2. Friend/block
   - Done: Add `UserBlock` domain, repository, DTO/API.
   - Endpoints: block, unblock, list blocked users.
   - Enforce block in friend request, user search/profile visibility where appropriate, and direct message sending.

3. Message idempotency
   - Done: Add optional `clientMessageId` to REST and STOMP message DTOs.
   - Persist it on `Message`.
   - Return existing message when the same sender sends same `clientMessageId` in same room.
   - Add Mongo index for `{ roomId, senderId, clientMessageId }` when present.

4. Group owner/admin basics
   - Done: Add transfer owner endpoint.
   - Add promote/demote admin endpoints.
   - Add member list endpoint.
   - Prevent owner/admin privilege bypass.
   - Add system messages for group management actions where practical.

5. Group settings basics
   - Done: Add embedded settings to `Room`: send message permission, invite permission, read history flag.
   - Enforce `ADMIN_ONLY` send setting for normal messages and attachments.
   - Enforce invite permission for direct add/invite request creation.

6. Group invite approval
   - Done: Add group join/invitation request model.
   - Member can invite/request another user when setting permits.
   - Admin/owner approves or rejects before target becomes member.
   - Notify target/admins through existing notification realtime queue.

7. Tests
   - Unit tests for block, group permission/admin transfer, message idempotency.
   - Keep existing 118-test baseline green.

## Phase 2 sau khi MVP on

1. Normalize conversation members
   - Add member-state collection with role, joinedAt, leftAt, lastReadAt, muteUntil, pin/hidden.
   - Migrate existing `Room.memberIds/admins/ownerId` safely.

2. Message receipts
   - Move delivered/read state to `message_receipts`.
   - Keep response compatibility.

3. Attachment hardening
   - Add antivirus/malware scanning adapter.
   - Store uploaderId and richer media metadata.

4. Reply/forward/reaction/mention/pin
   - Add forward API and `forwardedFromMessageId`.
   - Add reaction model/API.
   - Add mention parsing and prioritized notifications.
   - Add pinned message model/API with group permission enforcement.

5. Invite links
   - Add secure random group invite tokens, expiry, max usage, revoke, join request integration.

6. Notification expansion
   - Mention, invitation, poll, reminder, task notifications.
   - Respect mute except high-priority mention.

7. Audit log
   - Log group role changes, member changes, recall, block/unblock.

## Phase 3 thiet ke mo rong

1. Community conversations.
2. Poll, reminder, and task full modules.
3. Advanced moderation: ban list, retention policy, export, compliance.
4. Search indexes for file/link/media and richer conversation search.
