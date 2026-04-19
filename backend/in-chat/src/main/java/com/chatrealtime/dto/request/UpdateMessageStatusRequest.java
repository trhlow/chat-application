package com.chatrealtime.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateMessageStatusRequest(
        @NotBlank(message = "status is required")
        String status
) {
}


