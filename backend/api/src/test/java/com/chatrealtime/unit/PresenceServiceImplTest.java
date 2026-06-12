package com.chatrealtime.unit;

import com.chatrealtime.domain.User;
import com.chatrealtime.presence.PresenceStateStore;
import com.chatrealtime.realtime.PresenceRealtimeEventBus;
import com.chatrealtime.repository.UserRepository;
import com.chatrealtime.service.impl.PresenceServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresenceServiceImplTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private PresenceStateStore presenceStateStore;
    @Mock
    private PresenceRealtimeEventBus presenceRealtimeEventBus;

    @Test
    void markSessionOffline_WhenOtherSessionsRemain_ShouldNotPersistOffline() {
        PresenceServiceImpl service = new PresenceServiceImpl(
                userRepository,
                presenceStateStore,
                presenceRealtimeEventBus
        );
        when(presenceStateStore.unregisterSession("u1", "s1")).thenReturn(false);

        service.markSessionOffline("u1", "s1");

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
        verify(presenceRealtimeEventBus, never()).publish(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void markSessionOffline_WhenLastSessionClosed_ShouldPersistOffline() {
        PresenceServiceImpl service = new PresenceServiceImpl(
                userRepository,
                presenceStateStore,
                presenceRealtimeEventBus
        );
        User user = User.builder().id("u1").username("alice").isOnline(true).build();
        when(presenceStateStore.unregisterSession("u1", "s2")).thenReturn(true);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.markSessionOffline("u1", "s2");

        verify(userRepository).save(org.mockito.ArgumentMatchers.argThat(saved -> !saved.isOnline()));
        verify(presenceRealtimeEventBus).publish(org.mockito.ArgumentMatchers.argThat(event -> !event.online()));
    }
}
