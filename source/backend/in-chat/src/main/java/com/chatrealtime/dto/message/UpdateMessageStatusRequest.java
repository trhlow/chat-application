package com.chatrealtime.dto.message;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateMessageStatusRequest {
    @NotBlank(message = "status is required")
    private String status;
}
