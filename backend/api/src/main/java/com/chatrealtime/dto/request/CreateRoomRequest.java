package com.chatrealtime.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateRoomRequest(
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,
        @NotBlank(message = "type is required")
        String type,
        @NotEmpty(message = "memberIds is required")
        List<String> memberIds
) {
}


