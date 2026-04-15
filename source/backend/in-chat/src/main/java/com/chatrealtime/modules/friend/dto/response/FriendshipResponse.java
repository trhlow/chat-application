package com.chatrealtime.modules.friend.dto.response;

import com.chatrealtime.modules.user.dto.response.UserProfileResponse;

import java.time.Instant;

public record FriendshipResponse(
        String id,
        UserProfileResponse friend,
        Instant createdAt
) {
}
