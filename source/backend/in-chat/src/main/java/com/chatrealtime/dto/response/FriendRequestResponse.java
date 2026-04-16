package com.chatrealtime.dto.response;

import com.chatrealtime.domain.FriendRequestStatus;

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
