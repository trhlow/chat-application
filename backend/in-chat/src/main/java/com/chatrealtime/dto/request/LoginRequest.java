package com.chatrealtime.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        String username,
        String email,
        @NotBlank(message = "password is required")
        String password
) {
}


