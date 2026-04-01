package com.chatrealtime.controller;

import com.chatrealtime.service.AuthService;
import com.chatrealtime.model.User;
import com.chatrealtime.dto.auth.RegisterRequest;
import lombok.RequirArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequirArgsConstructor

public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public User registerUser(@RequestBody RegisterRequest request){
        return authService.register(request);
    }

}
