package com.chatrealtime.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RealtimeMessageRequest(
        @NotBlank(message = "content is required")
        @Size(max = 4000, message = "content must be at most 4000 characters")
        String content,

        @Size(max = 20, message = "type must be at most 20 characters")
        String type,

        String replyToMessageId
) {
    public RealtimeMessageRequest(String content) {
        this(content, null, null);
    }
}


