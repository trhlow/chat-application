package com.chatrealtime.service;

import com.chatrealtime.dto.auth.RegisterRequest;
import com.chatrealtime.model.User;
import com.chatrealtime.repository.UserRepository;
import lokbok.RequirArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequirArgsConstructor

public class AuthService {
    private final UserRepository userRepository;

    public User register(RegisterRequest request){
        if(userRepository.existsByUsername(request.getUsername())){
            throw new ExistsUsername("Tên đăng nhập đã tồn tại!")
        }

        User newUser = User.builder()
                .username(request.getUsername())
                .passwork(request.getPasswork())
                .email(request.getEmail())
                .avatar(request.getAvatar())
                .isOnline(false)
                .build();

        return userRepository.save(newUser);
    }
}
