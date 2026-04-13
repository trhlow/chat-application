package com.chatrealtime.modules.auth.dto;

import lombok.Data;

@Data
public class LogoutRequest {
    private String refreshToken;
}
