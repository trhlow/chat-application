package com.chatrealtime.service;

import com.chatrealtime.dto.request.LoginRequest;
import com.chatrealtime.dto.request.LogoutRequest;
import com.chatrealtime.dto.request.RefreshTokenRequest;
import com.chatrealtime.dto.request.RegisterRequest;
import com.chatrealtime.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    void logout(LogoutRequest request);

    void logoutAll();
}
