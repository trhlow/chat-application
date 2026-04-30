package com.chatrealtime.dto.response;

import java.time.Instant;

public record FriendshipResponse(
        String id,
        FriendUserResponse friend,
        Instant createdAt
) {
}
