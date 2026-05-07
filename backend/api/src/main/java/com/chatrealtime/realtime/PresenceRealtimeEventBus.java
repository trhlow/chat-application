package com.chatrealtime.realtime;

import com.chatrealtime.config.AppRedisProperties;
import com.chatrealtime.domain.Friendship;
import com.chatrealtime.domain.Room;
import com.chatrealtime.domain.User;
import com.chatrealtime.dto.response.PresenceResponse;
import com.chatrealtime.repository.FriendshipRepository;
import com.chatrealtime.repository.RoomRepository;
import com.chatrealtime.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PresenceRealtimeEventBus {
    private static final String PRESENCE_USER_QUEUE = "/queue/presence";

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final AppRedisProperties redisProperties;
    private final StringRedisTemplate stringRedisTemplate;
    private final FriendshipRepository friendshipRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public void publish(PresenceResponse event) {
        if (!redisProperties.enabled()) {
            sendLocally(event);
            return;
        }
        stringRedisTemplate.convertAndSend(redisProperties.channels().presence(), write(event));
    }

    @SuppressWarnings("unused")
    public void onRedisMessage(Message message, byte[] pattern) {
        sendLocally(read(message, PresenceResponse.class));
    }

    private void sendLocally(PresenceResponse event) {
        deliverToRecipients(event);
    }

    /**
     * Delivers presence to the subject user, their friends (canonical friendship pair), and other members
     * of rooms the subject belongs to. Uses per-user queues only (no global topic).
     */
    private void deliverToRecipients(PresenceResponse event) {
        String subjectId = event.userId();
        LinkedHashSet<String> recipientIds = new LinkedHashSet<>();
        recipientIds.add(subjectId);

        List<Friendship> friendships = friendshipRepository.findByUserIdAOrUserIdB(subjectId, subjectId);
        for (Friendship friendship : friendships) {
            if (subjectId.equals(friendship.getUserIdA())) {
                recipientIds.add(friendship.getUserIdB());
            } else if (subjectId.equals(friendship.getUserIdB())) {
                recipientIds.add(friendship.getUserIdA());
            }
        }

        List<Room> rooms = roomRepository.findByMemberIdsContaining(subjectId);
        for (Room room : rooms) {
            if (room.getMemberIds() == null) {
                continue;
            }
            for (String memberId : room.getMemberIds()) {
                if (!subjectId.equals(memberId)) {
                    recipientIds.add(memberId);
                }
            }
        }

        List<User> users = userRepository.findAllById(recipientIds);
        Map<String, User> byId = users.stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left));
        for (String recipientId : recipientIds) {
            User user = byId.get(recipientId);
            if (user == null || user.getUsername() == null || user.getUsername().isBlank()) {
                continue;
            }
            messagingTemplate.convertAndSendToUser(user.getUsername(), PRESENCE_USER_QUEUE, event);
        }
    }

    private String write(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize Redis realtime payload", exception);
        }
    }

    private <T> T read(Message message, Class<T> type) {
        try {
            return objectMapper.readValue(message.getBody(), type);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not deserialize Redis realtime payload", exception);
        }
    }
}
