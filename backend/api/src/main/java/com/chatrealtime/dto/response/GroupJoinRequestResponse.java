package com.chatrealtime.dto.response;

import com.chatrealtime.domain.GroupJoinRequestStatus;

import java.time.Instant;

public record GroupJoinRequestResponse(
        String id,
        String roomId,
        String requesterId,
        String targetUserId,
        GroupJoinRequestStatus status,
        Instant createdAt,
        Instant respondedAt,
        String respondedBy
) {
}
