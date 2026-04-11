package com.chatrealtime.dto.message;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateMessageRequest {
    @NotBlank(message = "roomId is required")
    private String roomId;

    @NotBlank(message = "senderId is required")
    private String senderId;

    @NotBlank(message = "content is required")
    private String content;
}
