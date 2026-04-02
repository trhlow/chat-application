package com.chatrealtime.service;

import com.chatrealtime.dto.auth.RegisterRequest;
import com.chatrealtime.exception.ExistsEmailException;
import com.chatrealtime.exception.ExistsUsernameException;
import com.chatrealtime.model.User;
import com.chatrealtime.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;

    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ExistsUsernameException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ExistsEmailException("Email already exists");
        }

        User newUser = User.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .email(request.getEmail())
                .avatar(request.getAvatar())
                .isOnline(false)
                .build();

        return userRepository.save(newUser);
    }
}

