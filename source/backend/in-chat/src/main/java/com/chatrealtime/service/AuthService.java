package com.chatrealtime.service;

import com.chatrealtime.dto.auth.LoginRequest;
import com.chatrealtime.dto.auth.RegisterRequest;
import com.chatrealtime.exception.ExistsEmailException;
import com.chatrealtime.exception.ExistsUsernameException;
import com.chatrealtime.exception.InvalidCredentialsException;
import com.chatrealtime.exception.UserNotFoundException;
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

    public User login(LoginRequest request) {
        User user = findByEmailOrUsername(request.getEmail(), request.getUsername());

        if (!user.getPassword().equals(request.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        user.setOnline(true);
        return userRepository.save(user);
    }

    public User logout(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setOnline(false);
        return userRepository.save(user);
    }

    private User findByEmailOrUsername(String email, String username) {
        if (email != null && !email.isBlank()) {
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));
        }

        if (username != null && !username.isBlank()) {
            return userRepository.findByUsername(username)
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));
        }

        throw new InvalidCredentialsException("Email or username is required");
    }
}
