package com.chatrealtime.modules.friend.dto.response;

import com.chatrealtime.modules.friend.model.FriendRequestStatus;
import com.chatrealtime.modules.user.dto.response.UserProfileResponse;

import java.time.Instant;

public record FriendRequestResponse(
        String id,
        UserProfileResponse requester,
        UserProfileResponse receiver,
        FriendRequestStatus status,
        Instant createdAt,
        Instant respondedAt
) {
}
