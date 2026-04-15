package com.chatrealtime.modules.friend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateFriendRequestRequest {
    @NotBlank(message = "receiverId is required")
    private String receiverId;
}
