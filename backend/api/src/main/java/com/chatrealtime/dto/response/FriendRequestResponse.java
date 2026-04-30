package com.chatrealtime.dto.response;

import com.chatrealtime.domain.FriendRequestStatus;

import java.time.Instant;

public record FriendRequestResponse(
        String id,
        FriendUserResponse requester,
        FriendUserResponse receiver,
        FriendRequestStatus status,
        Instant createdAt,
        Instant respondedAt
) {
}
