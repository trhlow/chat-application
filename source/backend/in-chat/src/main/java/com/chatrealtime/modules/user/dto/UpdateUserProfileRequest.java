package com.chatrealtime.modules.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @Size(min = 3, max = 50, message = "username must be between 3 and 50 characters")
        String username,
        @Size(max = 80, message = "displayName must be at most 80 characters")
        String displayName,
        @Size(max = 300, message = "bio must be at most 300 characters")
        String bio,
        @Size(max = 20, message = "phone must be at most 20 characters")
        String phone,
        @Pattern(regexp = "^(light|dark|system)$", message = "themePreference must be light, dark, or system")
        String themePreference
) {}



