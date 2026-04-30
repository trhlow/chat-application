package com.chatrealtime.dto.response;

public record PublicUserProfileResponse(
        String id,
        String username,
        String displayName,
        String avatarEndpoint
) {
}
