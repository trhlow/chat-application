package com.chatrealtime.security;

import com.chatrealtime.exception.UserNotFoundException;
import com.chatrealtime.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserPrincipalService implements UserDetailsService {
    public static final String CACHE_PRINCIPAL_BY_ID = "user-principals-by-id";
    public static final String CACHE_PRINCIPAL_BY_USERNAME = "user-principals-by-username";

    private final UserRepository userRepository;
    private final CacheManager cacheManager;

    @Override
    @Cacheable(cacheNames = CACHE_PRINCIPAL_BY_USERNAME, key = "#username.trim().toLowerCase()", sync = true)
    public UserDetails loadUserByUsername(String username) {
        return userRepository.findByUsername(username.trim().toLowerCase(Locale.ROOT))
                .map(AuthUserPrincipal::from)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    @Cacheable(cacheNames = CACHE_PRINCIPAL_BY_ID, key = "#userId", sync = true)
    public AuthUserPrincipal loadByUserId(String userId) {
        return userRepository.findById(userId)
                .map(AuthUserPrincipal::from)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    public void evictUserCaches(String userId, String username) {
        evict(CACHE_PRINCIPAL_BY_ID, userId);
        if (username != null && !username.isBlank()) {
            evict(CACHE_PRINCIPAL_BY_USERNAME, username.trim().toLowerCase(Locale.ROOT));
        }
    }

    private void evict(String cacheName, String key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }
}

