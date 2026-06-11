package com.chatrealtime.presence;

import com.chatrealtime.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DatabasePresenceStateStore implements PresenceStateStore {
    private final UserRepository userRepository;
    private final ConcurrentHashMap<String, Set<String>> sessionsByUserId = new ConcurrentHashMap<>();

    @Override
    public void markOnline(String userId) {
    }

    @Override
    public void markOffline(String userId) {
        sessionsByUserId.remove(userId);
    }

    @Override
    public boolean registerSession(String userId, String sessionId) {
        Set<String> sessions = sessionsByUserId.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet());
        boolean wasEmpty = sessions.isEmpty();
        sessions.add(sessionId);
        return wasEmpty;
    }

    @Override
    public boolean unregisterSession(String userId, String sessionId) {
        Set<String> sessions = sessionsByUserId.get(userId);
        if (sessions == null) {
            return true;
        }
        sessions.remove(sessionId);
        if (sessions.isEmpty()) {
            sessionsByUserId.remove(userId, sessions);
            return true;
        }
        return false;
    }

    @Override
    public boolean isOnline(String userId) {
        return userRepository.findById(userId).map(user -> user.isOnline()).orElse(false);
    }
}
