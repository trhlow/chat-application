package com.chatrealtime.unit;

import com.chatrealtime.domain.Message;
import com.chatrealtime.domain.MessageAttachment;
import com.chatrealtime.domain.Room;
import com.chatrealtime.domain.User;
import com.chatrealtime.dto.request.CreateMessageRequest;
import com.chatrealtime.dto.response.MessageResponse;
import com.chatrealtime.dto.response.RoomUnreadCountResponse;
import com.chatrealtime.exception.BadRequestException;
import com.chatrealtime.mapper.MessageMapper;
import com.chatrealtime.repository.MessageAttachmentRepository;
import com.chatrealtime.repository.MessageRepository;
import com.chatrealtime.repository.RoomRepository;
import com.chatrealtime.repository.UserRepository;
import com.chatrealtime.security.AuthContextService;
import com.chatrealtime.security.AuthUserPrincipal;
import com.chatrealtime.service.NotificationService;
import com.chatrealtime.service.PresenceService;
import com.chatrealtime.service.impl.MessageServiceImpl;
import com.chatrealtime.storage.MessageAttachmentStorageService;
import com.chatrealtime.storage.StoredMessageAttachment;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private MessageAttachmentRepository messageAttachmentRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private MessageMapper messageMapper;
    @Mock
    private MessageAttachmentStorageService messageAttachmentStorageService;
    @Mock
    private AuthContextService authContextService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private PresenceService presenceService;

    @InjectMocks
    private MessageServiceImpl messageService;

    @Test
    void createMessage_ShouldRejectWhenCurrentUserIsNotMember() {
        when(authContextService.requireCurrentUser()).thenReturn(new AuthUserPrincipal("u1", "alice", "pw", 0));
        when(roomRepository.findById("r1"))
                .thenReturn(Optional.of(Room.builder().id("r1").memberIds(List.of("u2")).build()));

        CreateMessageRequest request = new CreateMessageRequest("r1", "hello");

        assertThatThrownBy(() -> messageService.createMessage(request))
                .isInstanceOf(BadRequestException.class);
        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void updateMessageStatus_ShouldMarkAsSeenWhenAllRecipientsSeen() {
        when(authContextService.requireCurrentUser()).thenReturn(new AuthUserPrincipal("u2", "bob", "pw", 0));

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
        when(messageAttachmentRepository.findByMessageId("m1")).thenReturn(List.of());
        when(messageMapper.toResponse(any(Message.class), any())).thenAnswer(invocation -> {
            Message saved = invocation.getArgument(0);
            return new MessageResponse(
                    saved.getId(),
                    saved.getRoomId(),
                    saved.getSenderId(),
                    saved.getContent(),
                    saved.getTimestamp(),
                    saved.getStatus(),
                    saved.getDeliveredToUserIds(),
                    saved.getReadByUserIds(),
                    List.of()
            );
        });

        MessageResponse response = messageService.updateMessageStatus("m1", "seen");

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        assertThat(captor.getValue().getReadByUserIds()).contains("u2");
        assertThat(captor.getValue().getStatus()).isEqualTo("seen");
        assertThat(response.status()).isEqualTo("seen");
    }

    @Test
    void createMessageWithAttachment_ShouldCreateMessageAndAttachment() {
        when(authContextService.requireCurrentUser()).thenReturn(new AuthUserPrincipal("u1", "alice", "pw", 0));
        when(roomRepository.findById("r1"))
                .thenReturn(Optional.of(Room.builder().id("r1").memberIds(List.of("u1", "u2")).build()));
        when(userRepository.findAllById(List.of("u1", "u2"))).thenReturn(List.of());

        Message savedMessage = Message.builder()
                .id("m1")
                .roomId("r1")
                .senderId("u1")
                .content("hello")
                .timestamp(LocalDateTime.now())
                .status("sent")
                .deliveredToUserIds(Set.of("u1"))
                .readByUserIds(Set.of("u1"))
                .build();
        MessageAttachment savedAttachment = MessageAttachment.builder()
                .id("a1")
                .messageId("m1")
                .fileUrl("http://localhost:8080/uploads/message-attachments/image/a.png")
                .fileType("image")
                .mimeType("image/png")
                .fileSize(3L)
                .originalName("avatar.png")
                .thumbnailUrl("http://localhost:8080/uploads/message-attachments/image/a.png")
                .build();
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[] {1, 2, 3});

        when(messageAttachmentStorageService.store("u1", file)).thenReturn(new StoredMessageAttachment(
                savedAttachment.getFileUrl(),
                savedAttachment.getFileType(),
                savedAttachment.getMimeType(),
                savedAttachment.getFileSize(),
                savedAttachment.getOriginalName(),
                savedAttachment.getThumbnailUrl(),
                "local",
                "image/a.png"
        ));
        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);
        when(messageAttachmentRepository.save(any(MessageAttachment.class))).thenReturn(savedAttachment);
        when(messageMapper.toResponse(savedMessage, List.of(savedAttachment))).thenReturn(new MessageResponse(
                savedMessage.getId(),
                savedMessage.getRoomId(),
                savedMessage.getSenderId(),
                savedMessage.getContent(),
                savedMessage.getTimestamp(),
                savedMessage.getStatus(),
                savedMessage.getDeliveredToUserIds(),
                savedMessage.getReadByUserIds(),
                List.of()
        ));

        MessageResponse response = messageService.createMessageWithAttachment("r1", " hello ", file);

        ArgumentCaptor<MessageAttachment> attachmentCaptor = ArgumentCaptor.forClass(MessageAttachment.class);
        verify(messageAttachmentRepository).save(attachmentCaptor.capture());
        verify(roomRepository).save(argThat(room -> "hello".equals(room.getLastMessagePreview()) && room.getLastMessageAt() != null));
        assertThat(attachmentCaptor.getValue().getMessageId()).isEqualTo("m1");
        assertThat(attachmentCaptor.getValue().getFileType()).isEqualTo("image");
        assertThat(response.id()).isEqualTo("m1");
    }

    @Test
    void createMessage_ShouldNotifyOfflineRecipients() {
        when(authContextService.requireCurrentUser()).thenReturn(new AuthUserPrincipal("u1", "alice", "pw", 0));
        Room room = Room.builder().id("r1").name("Project A").memberIds(List.of("u1", "u2")).build();
        when(roomRepository.findById("r1")).thenReturn(Optional.of(room));

        Message savedMessage = Message.builder()
                .id("m1")
                .roomId("r1")
                .senderId("u1")
                .content("hello")
                .timestamp(LocalDateTime.now())
                .status("sent")
                .deliveredToUserIds(Set.of("u1"))
                .readByUserIds(Set.of("u1"))
                .build();
        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findAllById(List.of("u1", "u2"))).thenReturn(List.of(
                User.builder().id("u1").username("alice").displayName("Alice").isOnline(true).build(),
                User.builder().id("u2").username("bob").displayName("Bob").isOnline(false).build()
        ));
        when(userRepository.findById("u1")).thenReturn(Optional.of(
                User.builder().id("u1").username("alice").displayName("Alice").isOnline(true).build()
        ));
        when(presenceService.isUserOnline("u2")).thenReturn(false);
        when(messageMapper.toResponse(savedMessage, List.of())).thenReturn(new MessageResponse(
                savedMessage.getId(),
                savedMessage.getRoomId(),
                savedMessage.getSenderId(),
                savedMessage.getContent(),
                savedMessage.getTimestamp(),
                savedMessage.getStatus(),
                savedMessage.getDeliveredToUserIds(),
                savedMessage.getReadByUserIds(),
                List.of()
        ));

        MessageResponse response = messageService.createMessage(new CreateMessageRequest("r1", "hello"));

        verify(notificationService).createSystemNotification(
                eq("u2"),
                eq("new_message"),
                eq("New message"),
                eq("Alice sent a message in Project A: hello"),
                eq("r1")
        );
        assertThat(response.id()).isEqualTo("m1");
    }

    @Test
    void markRoomAsRead_ShouldMarkUnreadMessagesAsSeen() {
        when(authContextService.requireCurrentUser()).thenReturn(new AuthUserPrincipal("u2", "bob", "pw", 0));

        Room room = Room.builder().id("r1").memberIds(List.of("u1", "u2")).build();
        Message unreadMessage = Message.builder()
                .id("m1")
                .roomId("r1")
                .senderId("u1")
                .content("hello")
                .timestamp(LocalDateTime.now())
                .status("delivered")
                .deliveredToUserIds(Set.of("u1", "u2"))
                .readByUserIds(Set.of("u1"))
                .build();

        when(roomRepository.findById("r1")).thenReturn(Optional.of(room));
        when(mongoTemplate.find(any(), eq(Message.class))).thenReturn(List.of(unreadMessage));
        when(messageRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(messageAttachmentRepository.findByMessageIdIn(List.of("m1"))).thenReturn(List.of());
        when(messageMapper.toResponse(any(Message.class), any())).thenAnswer(invocation -> {
            Message saved = invocation.getArgument(0);
            return new MessageResponse(
                    saved.getId(),
                    saved.getRoomId(),
                    saved.getSenderId(),
                    saved.getContent(),
                    saved.getTimestamp(),
                    saved.getStatus(),
                    saved.getDeliveredToUserIds(),
                    saved.getReadByUserIds(),
                    List.of()
            );
        });

        messageService.markRoomAsRead("r1");

        ArgumentCaptor<List<Message>> captor = ArgumentCaptor.forClass(List.class);
        verify(messageRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getStatus()).isEqualTo("seen");
        assertThat(captor.getValue().get(0).getReadByUserIds()).contains("u2");
    }

    @Test
    void getUnreadCounts_ShouldReturnCountsPerRoom() {
        when(authContextService.requireCurrentUser()).thenReturn(new AuthUserPrincipal("u2", "bob", "pw", 0));
        when(roomRepository.findByMemberIdsContaining("u2")).thenReturn(List.of(
                Room.builder().id("r1").build(),
                Room.builder().id("r2").build()
        ));
        @SuppressWarnings("unchecked")
        AggregationResults<Document> aggregationResults = new AggregationResults<>(
                List.of(new Document("_id", "r1").append("unreadCount", 2L)),
                new Document("ok", 1)
        );
        when(mongoTemplate.aggregate(org.mockito.ArgumentMatchers.any(Aggregation.class), eq("messages"), eq(Document.class)))
                .thenReturn(aggregationResults);

        List<RoomUnreadCountResponse> response = messageService.getUnreadCounts();

        assertThat(response).containsExactly(
                new RoomUnreadCountResponse("r1", 2L),
                new RoomUnreadCountResponse("r2", 0L)
        );
    }
}
