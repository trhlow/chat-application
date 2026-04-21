package com.chatrealtime.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "username is required")
        @Size(min = 3, max = 50, message = "username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "password is required")
        @Size(min = 8, max = 100, message = "password must be between 8 and 100 characters")
        String password,

        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        String email,

        @Size(max = 120, message = "displayName must be at most 120 characters")
        String displayName,

        @Size(max = 512, message = "avatar URL must be at most 512 characters")
        String avatar
) {
}


