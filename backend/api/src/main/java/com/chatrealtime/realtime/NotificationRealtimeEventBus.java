package com.chatrealtime.realtime;

import com.chatrealtime.config.AppRedisProperties;
import com.chatrealtime.dto.response.NotificationRealtimeEventResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationRealtimeEventBus {
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final AppRedisProperties redisProperties;
    private final StringRedisTemplate stringRedisTemplate;

    public void publish(String destinationUsername, NotificationRealtimeEventResponse event) {
        NotificationRealtimeDelivery delivery = new NotificationRealtimeDelivery(destinationUsername, event);
        if (!redisProperties.enabled()) {
            sendLocally(delivery);
            return;
        }
        stringRedisTemplate.convertAndSend(redisProperties.channels().notification(), write(delivery));
    }

    @SuppressWarnings("unused")
    public void onRedisMessage(Message message, byte[] pattern) {
        sendLocally(read(message, NotificationRealtimeDelivery.class));
    }

    private void sendLocally(NotificationRealtimeDelivery delivery) {
        messagingTemplate.convertAndSendToUser(
                delivery.destinationUsername(),
                "/queue/notifications",
                delivery.event()
        );
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
