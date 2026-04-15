package com.chatrealtime.modules.friend.service;

import com.chatrealtime.exception.BadRequestException;
import com.chatrealtime.modules.friend.dto.CreateFriendRequestRequest;
import com.chatrealtime.modules.friend.dto.response.FriendRequestResponse;
import com.chatrealtime.modules.friend.mapper.FriendMapper;
import com.chatrealtime.modules.friend.model.FriendRequest;
import com.chatrealtime.modules.friend.model.FriendRequestStatus;
import com.chatrealtime.modules.friend.model.Friendship;
import com.chatrealtime.modules.friend.repository.FriendRequestRepository;
import com.chatrealtime.modules.friend.repository.FriendshipRepository;
import com.chatrealtime.modules.notification.service.NotificationService;
import com.chatrealtime.modules.user.model.User;
import com.chatrealtime.modules.user.repository.UserRepository;
import com.chatrealtime.security.AuthContextService;
import com.chatrealtime.security.AuthUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendServiceTest {
    @Mock
    private FriendRequestRepository friendRequestRepository;
    @Mock
    private FriendshipRepository friendshipRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FriendMapper friendMapper;
    @Mock
    private AuthContextService authContextService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private FriendService friendService;

    @Test
    void sendFriendRequest_ShouldRejectSelfRequest() {
        User currentUser = user("u1", "alice");
        CreateFriendRequestRequest request = new CreateFriendRequestRequest();
        request.setReceiverId("u1");

        when(authContextService.requireCurrentUser()).thenReturn(new AuthUserPrincipal("u1", "alice", "pw", 0));
        when(userRepository.findById("u1")).thenReturn(Optional.of(currentUser));

        assertThatThrownBy(() -> friendService.sendFriendRequest(request))
                .isInstanceOf(BadRequestException.class);
        verify(friendRequestRepository, never()).save(any(FriendRequest.class));
    }

    @Test
    void sendFriendRequest_ShouldRejectWhenAlreadyFriends() {
        User requester = user("u1", "alice");
        User receiver = user("u2", "bob");
        CreateFriendRequestRequest request = new CreateFriendRequestRequest();
        request.setReceiverId("u2");

        when(authContextService.requireCurrentUser()).thenReturn(new AuthUserPrincipal("u1", "alice", "pw", 0));
        when(userRepository.findById("u1")).thenReturn(Optional.of(requester));
        when(userRepository.findById("u2")).thenReturn(Optional.of(receiver));
        when(friendshipRepository.existsByUserIdAAndUserIdB("u1", "u2")).thenReturn(true);

        assertThatThrownBy(() -> friendService.sendFriendRequest(request))
                .isInstanceOf(BadRequestException.class);
        verify(friendRequestRepository, never()).save(any(FriendRequest.class));
    }

    @Test
    void acceptRequest_ShouldCreateFriendshipAndNotifyRequester() {
        User requester = user("u1", "alice");
        User receiver = user("u2", "bob");
        FriendRequest friendRequest = FriendRequest.builder()
                .id("fr1")
                .requesterId("u1")
                .receiverId("u2")
                .status(FriendRequestStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        when(authContextService.requireCurrentUser()).thenReturn(new AuthUserPrincipal("u2", "bob", "pw", 0));
        when(userRepository.findById("u2")).thenReturn(Optional.of(receiver));
        when(userRepository.findById("u1")).thenReturn(Optional.of(requester));
        when(friendRequestRepository.findById("fr1")).thenReturn(Optional.of(friendRequest));
        when(friendshipRepository.existsByUserIdAAndUserIdB("u1", "u2")).thenReturn(false);
        when(friendRequestRepository.save(any(FriendRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(friendMapper.toFriendRequestResponse(any(FriendRequest.class), eq(requester), eq(receiver)))
                .thenReturn(new FriendRequestResponse("fr1", null, null, FriendRequestStatus.ACCEPTED, friendRequest.getCreatedAt(), Instant.now()));

        FriendRequestResponse response = friendService.acceptRequest("fr1");

        ArgumentCaptor<Friendship> friendshipCaptor = ArgumentCaptor.forClass(Friendship.class);
        verify(friendshipRepository).save(friendshipCaptor.capture());
        assertThat(friendshipCaptor.getValue().getUserIdA()).isEqualTo("u1");
        assertThat(friendshipCaptor.getValue().getUserIdB()).isEqualTo("u2");
        assertThat(friendRequest.getStatus()).isEqualTo(FriendRequestStatus.ACCEPTED);
        verify(notificationService).createSystemNotification(
                eq("u1"),
                eq("friend_request_accepted"),
                eq("Friend request accepted"),
                eq("bob accepted your friend request"),
                eq("fr1")
        );
        assertThat(response.status()).isEqualTo(FriendRequestStatus.ACCEPTED);
    }

    private User user(String id, String username) {
        return User.builder()
                .id(id)
                .username(username)
                .displayName(username)
                .email(username + "@example.com")
                .build();
    }
}
