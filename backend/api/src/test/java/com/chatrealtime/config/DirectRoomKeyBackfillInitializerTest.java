package com.chatrealtime.config;

import com.chatrealtime.domain.Room;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DirectRoomKeyBackfillInitializerTest {

    private final MongoTemplate mongoTemplate = mock(MongoTemplate.class);
    private final DirectRoomKeyBackfillInitializer initializer = new DirectRoomKeyBackfillInitializer(mongoTemplate);

    @Test
    void run_whenLegacyDirectRoomMissingDirectKey_shouldBackfillCanonicalKey() {
        Room room = Room.builder()
                .id("room-1")
                .type("direct")
                .memberIds(List.of("user-b", "user-a"))
                .build();
        when(mongoTemplate.find(any(Query.class), eq(Room.class))).thenReturn(List.of(room));

        initializer.run(null);

        assertThat(room.getDirectKey()).isEqualTo("user-a:user-b");
        verify(mongoTemplate).save(room);
    }

    @Test
    void run_whenDuplicateLegacyDirectRoomsExist_shouldFailFast() {
        Room first = Room.builder()
                .id("room-1")
                .type("direct")
                .memberIds(List.of("user-a", "user-b"))
                .build();
        Room second = Room.builder()
                .id("room-2")
                .type("direct")
                .memberIds(List.of("user-b", "user-a"))
                .build();
        when(mongoTemplate.find(any(Query.class), eq(Room.class))).thenReturn(List.of(first, second));

        assertThatThrownBy(() -> initializer.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate direct rooms");

        verify(mongoTemplate, never()).save(any(Room.class));
    }

    @Test
    void run_whenExistingDirectKeyDoesNotMatchMembers_shouldFailFast() {
        Room room = Room.builder()
                .id("room-1")
                .type("direct")
                .directKey("user-a:user-c")
                .memberIds(List.of("user-a", "user-b"))
                .build();
        when(mongoTemplate.find(any(Query.class), eq(Room.class))).thenReturn(List.of(room));

        assertThatThrownBy(() -> initializer.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not match");

        verify(mongoTemplate, never()).save(any(Room.class));
    }
}
