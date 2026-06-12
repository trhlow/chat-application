package com.chatrealtime.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateGroupJoinRequest(
        @NotBlank(message = "targetUserId is required")
        String targetUserId
) {
}
