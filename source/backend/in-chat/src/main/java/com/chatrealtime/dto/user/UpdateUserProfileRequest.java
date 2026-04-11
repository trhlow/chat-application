package com.chatrealtime.dto.user;

import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @Size(min = 3, max = 50, message = "username must be between 3 and 50 characters")
        String username,
        @Size(max = 512, message = "avatar URL must be at most 512 characters")
        String avatar
) {}

