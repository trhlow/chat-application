package com.chatrealtime.unit;

import com.chatrealtime.domain.Message;
import com.chatrealtime.dto.response.MessageResponse;
import com.chatrealtime.mapper.MessageMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MessageMapperTest {
    private final MessageMapper mapper = new MessageMapper();

    @Test
    void toResponse_shouldNotExposeReceiptUserIds() {
        Message message = Message.builder()
                .id("m1")
                .roomId("r1")
                .senderId("u1")
                .content("hello")
                .timestamp(LocalDateTime.now())
                .status("seen")
                .deliveredToUserIds(Set.of("u1", "u2"))
                .readByUserIds(Set.of("u1", "u2"))
                .build();

        MessageResponse response = mapper.toResponse(message);

        assertThat(response.deliveredToUserIds()).isEmpty();
        assertThat(response.readByUserIds()).isEmpty();
        assertThat(response.status()).isEqualTo("seen");
    }
}
