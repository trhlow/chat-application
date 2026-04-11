package com.chatrealtime.dto.room.response;

import java.time.Instant;
import java.util.List;

public record RoomResponse(
        String id,
        String name,
        String type,
        List<String> memberIds,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
