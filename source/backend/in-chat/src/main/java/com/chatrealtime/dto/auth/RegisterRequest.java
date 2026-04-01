package com.chatrealtime.dto.auth;

import lombok.data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String email;
    private String avatar;
}