package com.chatrealtime.unit;

import com.chatrealtime.config.AppRedisProperties;
import com.chatrealtime.domain.Friendship;
import com.chatrealtime.domain.Room;
import com.chatrealtime.domain.User;
import com.chatrealtime.dto.response.PresenceResponse;
import com.chatrealtime.realtime.PresenceRealtimeEventBus;
import com.chatrealtime.repository.FriendshipRepository;
import com.chatrealtime.repository.RoomRepository;
import com.chatrealtime.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresenceRealtimeEventBusTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private AppRedisProperties redisProperties;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private FriendshipRepository friendshipRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PresenceRealtimeEventBus presenceRealtimeEventBus;

    @Test
    void publish_whenRedisDisabled_sendsToSelfAndFriendsViaUserQueueOnly() {
        when(redisProperties.enabled()).thenReturn(false);

        PresenceResponse event = new PresenceResponse("u1", true, Instant.now());
        Friendship friendship = Friendship.builder().userIdA("u1").userIdB("u2").build();
        when(friendshipRepository.findByUserIdAOrUserIdB("u1", "u1")).thenReturn(List.of(friendship));
        when(roomRepository.findByMemberIdsContaining("u1")).thenReturn(List.of());

        User self = User.builder().id("u1").username("alice").build();
        User friendUser = User.builder().id("u2").username("bob").build();
        when(userRepository.findAllById(anyIterable())).thenReturn(List.of(self, friendUser));

        presenceRealtimeEventBus.publish(event);

        verify(messagingTemplate).convertAndSendToUser("alice", "/queue/presence", event);
        verify(messagingTemplate).convertAndSendToUser("bob", "/queue/presence", event);
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void publish_whenRedisDisabled_roomMembersReceivePresenceEvenWhenNotFriends() {
        when(redisProperties.enabled()).thenReturn(false);

        PresenceResponse event = new PresenceResponse("u1", true, Instant.now());
        when(friendshipRepository.findByUserIdAOrUserIdB("u1", "u1")).thenReturn(List.of());
        Room room = Room.builder().id("r1").memberIds(List.of("u1", "u2")).build();
        when(roomRepository.findByMemberIdsContaining("u1")).thenReturn(List.of(room));

        User self = User.builder().id("u1").username("alice").build();
        User coMember = User.builder().id("u2").username("bob").build();
        when(userRepository.findAllById(anyIterable())).thenReturn(List.of(self, coMember));

        presenceRealtimeEventBus.publish(event);

        verify(messagingTemplate).convertAndSendToUser("alice", "/queue/presence", event);
        verify(messagingTemplate).convertAndSendToUser("bob", "/queue/presence", event);
    }

    @Test
    void publish_whenRedisDisabled_andNoFriendsOrRooms_sendsOnlyToSelf() {
        when(redisProperties.enabled()).thenReturn(false);

        PresenceResponse event = new PresenceResponse("u1", false, Instant.now());
        when(friendshipRepository.findByUserIdAOrUserIdB("u1", "u1")).thenReturn(List.of());
        when(roomRepository.findByMemberIdsContaining("u1")).thenReturn(List.of());

        User self = User.builder().id("u1").username("alice").build();
        when(userRepository.findAllById(anyIterable())).thenReturn(List.of(self));

        presenceRealtimeEventBus.publish(event);

        verify(messagingTemplate).convertAndSendToUser("alice", "/queue/presence", event);
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void publish_whenRedisEnabled_forwardsToRedisChannelOnly() throws JsonProcessingException {
        when(redisProperties.enabled()).thenReturn(true);
        when(redisProperties.channels()).thenReturn(new AppRedisProperties.Channels(
                "presence-ch",
                "notification-ch"
        ));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        PresenceResponse event = new PresenceResponse("u1", true, Instant.now());

        presenceRealtimeEventBus.publish(event);

        verify(stringRedisTemplate).convertAndSend(eq("presence-ch"), eq("{}"));
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }
}
