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

    @Test
    void toResponse_whenMessageRecalled_shouldMaskContent() {
        Message message = Message.builder()
                .id("m1")
                .roomId("r1")
                .senderId("u1")
                .content("secret")
                .timestamp(LocalDateTime.now())
                .status("sent")
                .recalled(true)
                .recalledAt(LocalDateTime.now())
                .build();

        MessageResponse response = mapper.toResponse(message);

        assertThat(response.content()).isEqualTo("Tin nh\u1EAFn \u0111\u00E3 \u0111\u01B0\u1EE3c thu h\u1ED3i");
        assertThat(response.recalled()).isTrue();
        assertThat(response.recalledAt()).isNotNull();
    }

    @Test
    void toResponse_whenReplyTargetRecalled_shouldMaskReplyPreview() {
        Message message = Message.builder()
                .id("m2")
                .roomId("r1")
                .senderId("u2")
                .content("reply")
                .replyToMessageId("m1")
                .timestamp(LocalDateTime.now())
                .status("sent")
                .build();
        Message replied = Message.builder()
                .id("m1")
                .roomId("r1")
                .senderId("u1")
                .content("original secret")
                .timestamp(LocalDateTime.now())
                .status("sent")
                .recalled(true)
                .build();

        MessageResponse response = mapper.toResponse(message, java.util.List.of(), replied);

        assertThat(response.replyToMessageId()).isEqualTo("m1");
        assertThat(response.replyPreview()).isEqualTo("Tin nh\u1EAFn \u0111\u00E3 \u0111\u01B0\u1EE3c thu h\u1ED3i");
    }
}
