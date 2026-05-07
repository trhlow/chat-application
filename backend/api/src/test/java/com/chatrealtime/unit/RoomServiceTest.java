package com.chatrealtime.unit;

import com.chatrealtime.domain.Message;
import com.chatrealtime.domain.Room;
import com.chatrealtime.domain.User;
import com.chatrealtime.dto.request.AddRoomMembersRequest;
import com.chatrealtime.dto.request.CreateRoomRequest;
import com.chatrealtime.dto.request.UpdateRoomNameRequest;
import com.chatrealtime.dto.response.RoomResponse;
import com.chatrealtime.mapper.RoomMapper;
import com.chatrealtime.repository.MessageAttachmentRepository;
import com.chatrealtime.repository.MessageRepository;
import com.chatrealtime.repository.RoomRepository;
import com.chatrealtime.repository.UserRepository;
import com.chatrealtime.security.AuthContextService;
import com.chatrealtime.security.AuthUserPrincipal;
import com.chatrealtime.service.MessageService;
import com.chatrealtime.service.NotificationService;
import com.chatrealtime.service.impl.RoomServiceImpl;
import com.chatrealtime.storage.AvatarStorageService;
import com.chatrealtime.storage.AvatarUploadResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private MessageAttachmentRepository messageAttachmentRepository;
    @Mock
    private RoomMapper roomMapper;
    @Mock
    private AuthContextService authContextService;
    @Mock
    private MessageService messageService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AvatarStorageService avatarStorageService;
    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private RoomServiceImpl roomService;

    @Test
    void createRoom_ShouldReturnExistingDirectRoomInsteadOfCreatingDuplicate() {
        CreateRoomRequest request = new CreateRoomRequest(null, "direct", List.of("u2"));
        Room existingRoom = Room.builder()
                .id("r1")
                .type("direct")
                .memberIds(List.of("u1", "u2"))
                .build();

        when(authContextService.requireCurrentUser()).thenReturn(new AuthUserPrincipal("u1", "alice", "pw", 0));
        when(userRepository.existsById("u1")).thenReturn(true);
        when(userRepository.existsById("u2")).thenReturn(true);
        when(roomRepository.findByTypeAndMemberIdsContaining("direct", "u2")).thenReturn(List.of(existingRoom));
        when(roomMapper.toResponse(existingRoom, 0L)).thenReturn(toResponse(existingRoom, 0L));

        RoomResponse response = roomService.createRoom(request);

        verify(roomRepository, never()).save(any(Room.class));
        assertThat(response.id()).isEqualTo("r1");
    }

    @Test
    void createRoom_ShouldCreateGroupWithOwnerAdminAndNotifyMembers() {
        CreateRoomRequest request = new CreateRoomRequest("Project A", "group", List.of("u2", "u3"));
        User currentUser = User.builder().id("u1").username("alice").displayName("Alice").build();

        when(authContextService.requireCurrentUser()).thenReturn(new AuthUserPrincipal("u1", "alice", "pw", 0));
        when(userRepository.existsById("u1")).thenReturn(true);
        when(userRepository.existsById("u2")).thenReturn(true);
        when(userRepository.existsById("u3")).thenReturn(true);
        when(userRepository.findById("u1")).thenReturn(Optional.of(currentUser));
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> {
            Room room = invocation.getArgument(0);
            room.setId("g1");
            return room;
        });
        when(roomMapper.toResponse(any(Room.class), eq(0L))).thenAnswer(invocation -> toResponse(invocation.getArgument(0), invocation.getArgument(1)));

        RoomResponse response = roomService.createRoom(request);

        ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(roomCaptor.capture());
        assertThat(roomCaptor.getValue().getOwnerId()).isEqualTo("u1");
        assertThat(roomCaptor.getValue().getAdmins()).containsExactly("u1");
        verify(notificationService).createSystemNotification(eq("u2"), eq("group_member_added"), eq("Added to group"), eq("Alice added you to group Project A"), eq("g1"));
        verify(notificationService).createSystemNotification(eq("u3"), eq("group_member_added"), eq("Added to group"), eq("Alice added you to group Project A"), eq("g1"));
        assertThat(response.ownerId()).isEqualTo("u1");
    }

    @Test
    void addMembers_ShouldAppendNewMembersAndNotifyThem() {
        Room room = Room.builder()
                .id("g1")
                .name("Project A")
                .type("group")
                .memberIds(List.of("u1", "u2"))
                .admins(List.of("u1"))
                .ownerId("u1")
                .createdBy("u1")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        User currentUser = User.builder().id("u1").username("alice").displayName("Alice").build();

        when(authContextService.requireCurrentUser()).thenReturn(new AuthUserPrincipal("u1", "alice", "pw", 0));
        when(roomRepository.findById("g1")).thenReturn(Optional.of(room));
        when(userRepository.existsById("u3")).thenReturn(true);
        when(userRepository.findById("u1")).thenReturn(Optional.of(currentUser));
        when(messageService.getUnreadCountMap(List.of("g1"))).thenReturn(Map.of("g1", 4L));
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roomMapper.toResponse(any(Room.class), eq(4L))).thenAnswer(invocation -> toResponse(invocation.getArgument(0), invocation.getArgument(1)));

        RoomResponse response = roomService.addMembers("g1", new AddRoomMembersRequest(List.of("u2", "u3")));

        assertThat(response.memberIds()).containsExactly("u1", "u2", "u3");
        assertThat(response.unreadCount()).isEqualTo(4L);
        verify(notificationService).createSystemNotification(eq("u3"), eq("group_member_added"), eq("Added to group"), eq("Alice added you to group Project A"), eq("g1"));
    }

    @Test
    void getRoomById_WhenCurrentUserIsNotMember_ShouldThrowAccessDenied() {
        Room room = Room.builder()
                .id("g1")
                .type("group")
                .memberIds(List.of("u2", "u3"))
                .build();

        when(authContextService.requireCurrentUser()).thenReturn(new AuthUserPrincipal("u1", "alice", "pw", 0));
        when(roomRepository.findById("g1")).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> roomService.getRoomById("g1"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateRoomName_WhenCurrentUserIsNotAdmin_ShouldThrowAccessDenied() {
        Room room = Room.builder()
                .id("g1")
                .type("group")
                .memberIds(List.of("u1", "u2", "u3"))
                .admins(List.of("u2"))
                .ownerId("u2")
                .build();

        when(authContextService.requireCurrentUser()).thenReturn(new AuthUserPrincipal("u1", "alice", "pw", 0));
        when(roomRepository.findById("g1")).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> roomService.updateRoomName("g1", new UpdateRoomNameRequest("Renamed")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void dissolveRoom_WhenCurrentUserIsNotOwner_ShouldThrowAccessDenied() {
        Room room = Room.builder()
                .id("g1")
                .type("group")
                .memberIds(List.of("u1", "u2", "u3"))
                .admins(List.of("u1", "u2"))
                .ownerId("u2")
                .build();

        when(authContextService.requireCurrentUser()).thenReturn(new AuthUserPrincipal("u1", "alice", "pw", 0));
        when(roomRepository.findById("g1")).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> roomService.dissolveRoom("g1"))
                .isInstanceOf(AccessDeniedException.class);
        verify(messageRepository, never()).findByRoomId("g1");
    }

    @Test
    void dissolveRoom_WhenOwner_ShouldDeleteMessagesInBatchesWithoutLoadingEntireRoom() {
        Room room = Room.builder()
                .id("g1")
                .type("group")
                .memberIds(List.of("u1", "u2", "u3"))
                .admins(List.of("u1"))
                .ownerId("u1")
                .avatarProvider("local")
                .avatarPublicId("room-avatar.png")
                .build();
        Message message = Message.builder().id("m1").roomId("g1").build();

        when(authContextService.requireCurrentUser()).thenReturn(new AuthUserPrincipal("u1", "alice", "pw", 0));
        when(roomRepository.findById("g1")).thenReturn(Optional.of(room));
        when(mongoTemplate.find(any(Query.class), eq(Message.class)))
                .thenReturn(List.of(message))
                .thenReturn(List.of());

        roomService.dissolveRoom("g1");

        verify(messageRepository, never()).findByRoomId("g1");
        verify(messageAttachmentRepository).deleteByMessageIdIn(List.of("m1"));
        verify(mongoTemplate, times(2)).find(any(Query.class), eq(Message.class));
        verify(mongoTemplate).remove(any(Query.class), eq(Message.class));
        verify(avatarStorageService).deleteAvatar("local", "room-avatar.png");
        verify(roomRepository).delete(room);
    }

    @Test
    void updateRoomAvatar_WhenSaveFails_ShouldDeleteNewUploadOnlyAndRethrow() {
        Room room = Room.builder()
                .id("g1")
                .name("Project A")
                .type("group")
                .memberIds(List.of("u1", "u2"))
                .admins(List.of("u1"))
                .ownerId("u1")
                .avatarProvider("local")
                .avatarPublicId("old-room.png")
                .build();
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        AvatarUploadResult uploaded = new AvatarUploadResult("http://local/new.png", "new-room.png", "local");

        when(authContextService.requireCurrentUser()).thenReturn(new AuthUserPrincipal("u1", "alice", "pw", 0));
        when(roomRepository.findById("g1")).thenReturn(Optional.of(room));
        when(avatarStorageService.uploadAvatar("room-g1", file)).thenReturn(uploaded);
        when(roomRepository.save(any(Room.class))).thenThrow(new RuntimeException("db"));

        assertThatThrownBy(() -> roomService.updateRoomAvatar("g1", file))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db");

        verify(avatarStorageService).deleteAvatar("local", "new-room.png");
        verify(avatarStorageService, never()).deleteAvatar("local", "old-room.png");
    }

    @Test
    void leaveRoom_ShouldTransferOwnershipWhenOwnerLeaves() {
        Room room = Room.builder()
                .id("g1")
                .name("Project A")
                .type("group")
                .memberIds(List.of("u1", "u2", "u3"))
                .admins(List.of("u1", "u2"))
                .ownerId("u1")
                .createdBy("u1")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(authContextService.requireCurrentUser()).thenReturn(new AuthUserPrincipal("u1", "alice", "pw", 0));
        when(roomRepository.findById("g1")).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        roomService.leaveRoom("g1");

        ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(roomCaptor.capture());
        assertThat(roomCaptor.getValue().getOwnerId()).isEqualTo("u2");
        assertThat(roomCaptor.getValue().getMemberIds()).containsExactly("u2", "u3");
        assertThat(roomCaptor.getValue().getAdmins()).containsExactly("u2");
    }

    private RoomResponse toResponse(Room room, long unreadCount) {
        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getType(),
                room.getAvatar() == null ? null : "/api/rooms/" + room.getId() + "/avatar",
                room.getAvatar() == null ? null : "/api/rooms/" + room.getId() + "/avatar",
                room.getMemberIds(),
                room.getAdmins(),
                room.getCreatedBy(),
                room.getOwnerId(),
                unreadCount,
                room.getLastMessageAt(),
                room.getLastMessagePreview(),
                room.getCreatedAt(),
                room.getUpdatedAt()
        );
    }
}
