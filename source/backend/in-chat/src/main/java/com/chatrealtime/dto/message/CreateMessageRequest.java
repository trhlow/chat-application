package com.chatrealtime.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateMessageRequest {
    @NotBlank(message = "roomId is required")
    private String roomId;

    @NotBlank(message = "content is required")
    @Size(max = 4000, message = "content must be at most 4000 characters")
    private String content;
}
