package com.chatrealtime.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMessageRequest(
        @NotBlank(message = "roomId is required")
        String roomId,

        @NotBlank(message = "content is required")
        @Size(max = 4000, message = "content must be at most 4000 characters")
        String content,

        @Size(max = 20, message = "type must be at most 20 characters")
        String type,

        String replyToMessageId
) {
    public CreateMessageRequest(String roomId, String content) {
        this(roomId, content, null, null);
    }
}


