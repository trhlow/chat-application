# Backend ZaloChat API Summary

## Existing APIs

### Auth

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `POST /api/auth/logout-all`

### Users

- `GET /api/users?query=...`
- `GET /api/users/me`
- `PUT /api/users/me`
- `POST /api/users/me/avatar`
- `GET /api/users/{userId}`
- `GET /api/users/{userId}/avatar`

### Friends

- `POST /api/friends/requests`
- `GET /api/friends/requests/incoming`
- `GET /api/friends/requests/outgoing`
- `POST /api/friends/requests/{id}/accept`
- `POST /api/friends/requests/{id}/reject`
- `DELETE /api/friends/requests/{id}`
- `GET /api/friends`
- `DELETE /api/friends/{friendId}`
- `POST /api/friends/blocks/{userId}`
- `DELETE /api/friends/blocks/{userId}`
- `GET /api/friends/blocks`

### Rooms/conversations

- `GET /api/rooms`
- `GET /api/rooms/{roomId}`
- `POST /api/rooms`
- `GET /api/rooms/{roomId}/messages`
- `GET /api/rooms/{roomId}/messages/search`
- `PUT /api/rooms/{roomId}/read`
- `GET /api/rooms/{roomId}/unread-count`
- `POST /api/rooms/{roomId}/attachments`
- `POST /api/rooms/{roomId}/members`
- `DELETE /api/rooms/{roomId}/members/{memberId}`
- `GET /api/rooms/{roomId}/members`
- `POST /api/rooms/{roomId}/admins/{memberId}`
- `DELETE /api/rooms/{roomId}/admins/{memberId}`
- `PUT /api/rooms/{roomId}/owner/{memberId}`
- `PATCH /api/rooms/{roomId}/settings`
- `POST /api/rooms/{roomId}/join-requests`
- `GET /api/rooms/{roomId}/join-requests`
- `POST /api/rooms/{roomId}/join-requests/{requestId}/approve`
- `POST /api/rooms/{roomId}/join-requests/{requestId}/reject`
- `PATCH /api/rooms/{roomId}/name`
- `GET /api/rooms/{roomId}/avatar`
- `POST /api/rooms/{roomId}/avatar`
- `POST /api/rooms/{roomId}/leave`
- `DELETE /api/rooms/{roomId}`

### Messages

- `GET /api/messages?roomId=...`
- `POST /api/messages`
- `POST /api/messages/attachments`
- `PATCH /api/messages/{messageId}/status`
- `PUT /api/messages/{messageId}/recall`
- `DELETE /api/messages/{messageId}/me`
- `POST /api/messages/rooms/{roomId}/read`
- `GET /api/messages/unread-counts`
- `GET /api/messages/{messageId}/attachments/{attachmentId}/download`

### Notifications

- `GET /api/notifications`
- `GET /api/notifications/unread-count`
- `PATCH /api/notifications/{notificationId}/read`
- `PATCH /api/notifications/read-all`

### STOMP

- Connect endpoint: `/ws`
- Send message: `/app/rooms/{roomId}/messages`
- Typing: `/app/rooms/{roomId}/typing`
- Message status: `/app/messages/{messageId}/status`
- Room topics: `/topic/rooms/{roomId}/messages`, `/topic/rooms/{roomId}/typing`, `/topic/rooms/{roomId}/status`
- User queues: `/user/queue/notifications`, `/user/queue/presence`

## API changes implemented in MVP pass

- Auth refresh/logout also support refresh token cookie.
- Message send DTOs accept optional `clientMessageId`.
- Friends API has block/unblock/list blocked endpoints.
- Room API has owner/admin transfer, member listing, settings, and invite approval endpoints.
