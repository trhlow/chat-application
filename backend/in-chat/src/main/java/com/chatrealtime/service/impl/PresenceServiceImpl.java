package com.chatrealtime.service.impl;

import com.chatrealtime.service.PresenceService;

import com.chatrealtime.dto.response.PresenceResponse;
import com.chatrealtime.domain.User;
import com.chatrealtime.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
@RequiredArgsConstructor
public class PresenceServiceImpl implements PresenceService {
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void markOnline(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setOnline(true);
            user.setUpdatedAt(Instant.now());
            User saved = userRepository.save(user);
            publishPresence(saved);
        });
    }

    @Override
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


