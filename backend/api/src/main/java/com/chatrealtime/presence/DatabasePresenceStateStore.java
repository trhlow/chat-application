package com.chatrealtime.presence;

import com.chatrealtime.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DatabasePresenceStateStore implements PresenceStateStore {
    private final UserRepository userRepository;

    @Override
    public void markOnline(String userId) {
    }

    @Override
    public void markOffline(String userId) {
    }

    @Override
    public boolean isOnline(String userId) {
        return userRepository.findById(userId).map(user -> user.isOnline()).orElse(false);
    }
}
