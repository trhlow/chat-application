package com.chatrealtime.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMessageRequest(
        @NotBlank(message = "roomId is required")
        String roomId,

        @NotBlank(message = "content is required")
        @Size(max = 4000, message = "content must be at most 4000 characters")
        String content
) {
}


