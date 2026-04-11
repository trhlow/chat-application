package com.chatrealtime.security;

import com.chatrealtime.exception.UserNotFoundException;
import com.chatrealtime.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserPrincipalService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        return userRepository.findByUsername(username.trim().toLowerCase(Locale.ROOT))
                .map(AuthUserPrincipal::from)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    public AuthUserPrincipal loadByUserId(String userId) {
        return userRepository.findById(userId)
                .map(AuthUserPrincipal::from)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
}

