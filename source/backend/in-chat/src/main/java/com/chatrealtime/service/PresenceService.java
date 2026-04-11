package com.chatrealtime.service;

import com.chatrealtime.dto.user.response.PresenceResponse;
import com.chatrealtime.model.User;
import com.chatrealtime.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PresenceService {
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public void markOnline(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setOnline(true);
            user.setUpdatedAt(Instant.now());
            User saved = userRepository.save(user);
            publishPresence(saved);
        });
    }

    public void markOffline(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            Instant now = Instant.now();
            user.setOnline(false);
            user.setLastSeenAt(now);
            user.setUpdatedAt(now);
            User saved = userRepository.save(user);
            publishPresence(saved);
        });
    }

    private void publishPresence(User user) {
        PresenceResponse event = new PresenceResponse(user.getId(), user.isOnline(), user.getLastSeenAt());
        messagingTemplate.convertAndSend("/topic/presence", event);
    }
}
