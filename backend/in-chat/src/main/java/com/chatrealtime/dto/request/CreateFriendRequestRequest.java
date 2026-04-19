package com.chatrealtime.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateFriendRequestRequest(
        @NotBlank(message = "receiverId is required")
        String receiverId
) {
}
