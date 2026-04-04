package com.chatrealtime.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @NotBlank(message = "Ten nguoi dung khong duoc de trong!")
        @Size(min = 8, max = 100, message = "Ten nguoi dung dai toi da tu 8 den 100 ky tu!")
        String username
) {}

