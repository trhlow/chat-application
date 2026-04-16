package com.chatrealtime.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateNotificationRequest(
        @NotBlank(message = "type is required")
        @Size(max = 50, message = "type must be at most 50 characters")
        String type,

        @NotBlank(message = "title is required")
        String title,

        @NotBlank(message = "message is required")
        @Size(max = 1000, message = "message must be at most 1000 characters")
        String message,

        String relatedId
) {
}
