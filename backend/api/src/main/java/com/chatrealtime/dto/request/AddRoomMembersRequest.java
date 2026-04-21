package com.chatrealtime.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AddRoomMembersRequest(
        @NotEmpty(message = "memberIds is required")
        List<String> memberIds
) {
}
