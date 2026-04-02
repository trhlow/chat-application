package com.chatrealtime.controller;

import com.chatrealtime.dto.auth.RegisterRequest;
import com.chatrealtime.model.User;
import com.chatrealtime.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public User registerUser(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }
}

