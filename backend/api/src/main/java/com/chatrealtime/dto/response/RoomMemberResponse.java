package com.chatrealtime.dto.response;

public record RoomMemberResponse(
        String id,
        String username,
        String displayName,
        String avatarEndpoint,
        @Deprecated(forRemoval = true)
        String avatar,
        String role
) {
}
