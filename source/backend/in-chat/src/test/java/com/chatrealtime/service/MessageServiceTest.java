package com.chatrealtime.modules.message.service;

import com.chatrealtime.modules.message.dto.CreateMessageRequest;
import com.chatrealtime.modules.message.dto.response.MessageResponse;
import com.chatrealtime.exception.BadRequestException;
import com.chatrealtime.modules.message.mapper.MessageMapper;
import com.chatrealtime.modules.message.model.Message;
import com.chatrealtime.modules.room.model.Room;
import com.chatrealtime.modules.message.repository.MessageRepository;
import com.chatrealtime.modules.room.repository.RoomRepository;
import com.chatrealtime.security.AuthContextService;
import com.chatrealtime.security.AuthUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private MessageMapper messageMapper;
    @Mock
    private AuthContextService authContextService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private MessageService messageService;

    @Test
    void createMessage_ShouldRejectWhenCurrentUserIsNotMember() {
        when(authContextService.requireCurrentUser()).thenReturn(new AuthUserPrincipal("u1", "alice", "pw"));
        when(roomRepository.findById("r1"))
                .thenReturn(Optional.of(Room.builder().id("r1").memberIds(List.of("u2")).build()));

        CreateMessageRequest request = new CreateMessageRequest();
        request.setRoomId("r1");
        request.setContent("hello");

        assertThatThrownBy(() -> messageService.createMessage(request))
                .isInstanceOf(BadRequestException.class);
        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void updateMessageStatus_ShouldMarkAsReadWhenAllRecipientsRead() {
        when(authContextService.requireCurrentUser()).thenReturn(new AuthUserPrincipal("u2", "bob", "pw"));

        Message message = Message.builder()
                .id("m1")
                .roomId("r1")
                .senderId("u1")
                .content("hello")
                .timestamp(LocalDateTime.now())
                .status("sent")
                .deliveredToUserIds(Set.of("u1"))
                .readByUserIds(Set.of("u1"))
                .build();
        Room room = Room.builder().id("r1").memberIds(List.of("u1", "u2")).build();

        when(messageRepository.findById("m1")).thenReturn(Optional.of(message));
        when(roomRepository.findById("r1")).thenReturn(Optional.of(room));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(messageMapper.toResponse(any(Message.class))).thenAnswer(invocation -> {
            Message saved = invocation.getArgument(0);
            return new MessageResponse(
                    saved.getId(),
                    saved.getRoomId(),
                    saved.getSenderId(),
                    saved.getContent(),
                    saved.getTimestamp(),
                    saved.getStatus(),
                    saved.getDeliveredToUserIds(),
                    saved.getReadByUserIds()
            );
        });

        MessageResponse response = messageService.updateMessageStatus("m1", "read");

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        assertThat(captor.getValue().getReadByUserIds()).contains("u2");
        assertThat(captor.getValue().getStatus()).isEqualTo("read");
        assertThat(response.status()).isEqualTo("read");
    }
}

