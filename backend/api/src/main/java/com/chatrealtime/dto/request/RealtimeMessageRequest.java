package com.chatrealtime.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RealtimeMessageRequest(
        @NotBlank(message = "content is required")
        @Size(max = 4000, message = "content must be at most 4000 characters")
        String content,

        @Size(max = 20, message = "type must be at most 20 characters")
        String type,

        String replyToMessageId,

        @Size(max = 100, message = "clientMessageId must be at most 100 characters")
        String clientMessageId
) {
    public RealtimeMessageRequest(String content) {
        this(content, null, null, null);
    }

    public RealtimeMessageRequest(String content, String type, String replyToMessageId) {
        this(content, type, replyToMessageId, null);
    }
}


