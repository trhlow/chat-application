package com.chatrealtime.modules.friend.service;

import com.chatrealtime.exception.BadRequestException;
import com.chatrealtime.exception.FriendRequestNotFoundException;
import com.chatrealtime.exception.FriendshipNotFoundException;
import com.chatrealtime.exception.UserNotFoundException;
import com.chatrealtime.modules.friend.dto.CreateFriendRequestRequest;
import com.chatrealtime.modules.friend.dto.response.FriendRequestResponse;
import com.chatrealtime.modules.friend.dto.response.FriendshipResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendService {
    private static final String NOTIFICATION_FRIEND_REQUEST = "friend_request";
    private static final String NOTIFICATION_FRIEND_ACCEPTED = "friend_request_accepted";

    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final FriendMapper friendMapper;
    private final AuthContextService authContextService;
    private final NotificationService notificationService;

    public FriendRequestResponse sendFriendRequest(CreateFriendRequestRequest request) {
        User requester = getCurrentUser();
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new UserNotFoundException("Receiver not found"));

        if (requester.getId().equals(receiver.getId())) {
            throw new BadRequestException("Cannot send a friend request to yourself");
        }

        NormalizedPair pair = normalizePair(requester.getId(), receiver.getId());
        if (friendshipRepository.existsByUserIdAAndUserIdB(pair.userIdA(), pair.userIdB())) {
            throw new BadRequestException("Users are already friends");
        }

        boolean hasPendingRequest = friendRequestRepository.existsByRequesterIdAndReceiverIdAndStatus(
                requester.getId(),
                receiver.getId(),
                FriendRequestStatus.PENDING
        ) || friendRequestRepository.existsByRequesterIdAndReceiverIdAndStatus(
                receiver.getId(),
                requester.getId(),
                FriendRequestStatus.PENDING
        );
        if (hasPendingRequest) {
            throw new BadRequestException("A pending friend request already exists");
        }

        FriendRequest savedRequest = friendRequestRepository.save(FriendRequest.builder()
                .requesterId(requester.getId())
                .receiverId(receiver.getId())
                .status(FriendRequestStatus.PENDING)
                .createdAt(Instant.now())
                .build());

        notificationService.createSystemNotification(
                receiver.getId(),
                NOTIFICATION_FRIEND_REQUEST,
                "New friend request",
                displayName(requester) + " sent you a friend request",
                savedRequest.getId()
        );

        return friendMapper.toFriendRequestResponse(savedRequest, requester, receiver);
    }

    public List<FriendRequestResponse> getIncomingRequests() {
        User currentUser = getCurrentUser();
        return friendRequestRepository.findByReceiverIdAndStatusOrderByCreatedAtDesc(
                        currentUser.getId(),
                        FriendRequestStatus.PENDING
                )
                .stream()
                .map(this::toFriendRequestResponse)
                .toList();
    }

    public List<FriendRequestResponse> getOutgoingRequests() {
        User currentUser = getCurrentUser();
        return friendRequestRepository.findByRequesterIdAndStatusOrderByCreatedAtDesc(
                        currentUser.getId(),
                        FriendRequestStatus.PENDING
                )
                .stream()
                .map(this::toFriendRequestResponse)
                .toList();
    }

    public FriendRequestResponse acceptRequest(String requestId) {
        User currentUser = getCurrentUser();
        FriendRequest friendRequest = getPendingRequest(requestId);

        if (!currentUser.getId().equals(friendRequest.getReceiverId())) {
            throw new BadRequestException("Only the receiver can accept this friend request");
        }

        User requester = getUser(friendRequest.getRequesterId());
        NormalizedPair pair = normalizePair(friendRequest.getRequesterId(), friendRequest.getReceiverId());
        if (!friendshipRepository.existsByUserIdAAndUserIdB(pair.userIdA(), pair.userIdB())) {
            friendshipRepository.save(Friendship.builder()
                    .userIdA(pair.userIdA())
                    .userIdB(pair.userIdB())
                    .userIds(List.of(pair.userIdA(), pair.userIdB()))
                    .createdAt(Instant.now())
                    .build());
        }

        friendRequest.setStatus(FriendRequestStatus.ACCEPTED);
        friendRequest.setRespondedAt(Instant.now());
        FriendRequest savedRequest = friendRequestRepository.save(friendRequest);

        notificationService.createSystemNotification(
                requester.getId(),
                NOTIFICATION_FRIEND_ACCEPTED,
                "Friend request accepted",
                displayName(currentUser) + " accepted your friend request",
                savedRequest.getId()
        );

        return friendMapper.toFriendRequestResponse(savedRequest, requester, currentUser);
    }

    public FriendRequestResponse rejectRequest(String requestId) {
        User currentUser = getCurrentUser();
        FriendRequest friendRequest = getPendingRequest(requestId);

        if (!currentUser.getId().equals(friendRequest.getReceiverId())) {
            throw new BadRequestException("Only the receiver can reject this friend request");
        }

        friendRequest.setStatus(FriendRequestStatus.REJECTED);
        friendRequest.setRespondedAt(Instant.now());
        FriendRequest savedRequest = friendRequestRepository.save(friendRequest);

        return friendMapper.toFriendRequestResponse(
                savedRequest,
                getUser(savedRequest.getRequesterId()),
                currentUser
        );
    }

    public FriendRequestResponse cancelRequest(String requestId) {
        User currentUser = getCurrentUser();
        FriendRequest friendRequest = getPendingRequest(requestId);

        if (!currentUser.getId().equals(friendRequest.getRequesterId())) {
            throw new BadRequestException("Only the requester can cancel this friend request");
        }

        friendRequest.setStatus(FriendRequestStatus.CANCELED);
        friendRequest.setRespondedAt(Instant.now());
        FriendRequest savedRequest = friendRequestRepository.save(friendRequest);

        return friendMapper.toFriendRequestResponse(
                savedRequest,
                currentUser,
                getUser(savedRequest.getReceiverId())
        );
    }

    public List<FriendshipResponse> getFriends() {
        User currentUser = getCurrentUser();
        List<Friendship> friendships = friendshipRepository.findByUserIdsContaining(currentUser.getId());
        List<String> friendIds = friendships.stream()
                .map(friendship -> otherUserId(friendship, currentUser.getId()))
                .toList();
        Map<String, User> friendsById = userRepository.findAllById(friendIds)
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return friendships.stream()
                .map(friendship -> friendMapper.toFriendshipResponse(
                        friendship,
                        getFriendFromMap(friendsById, otherUserId(friendship, currentUser.getId()))
                ))
                .toList();
    }

    public void removeFriend(String friendId) {
        User currentUser = getCurrentUser();
        getUser(friendId);
        NormalizedPair pair = normalizePair(currentUser.getId(), friendId);
        Friendship friendship = friendshipRepository.findByUserIdAAndUserIdB(pair.userIdA(), pair.userIdB())
                .orElseThrow(() -> new FriendshipNotFoundException("Friendship not found"));
        friendshipRepository.delete(friendship);
    }

    private FriendRequest getPendingRequest(String requestId) {
        FriendRequest friendRequest = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new FriendRequestNotFoundException("Friend request not found"));
        if (friendRequest.getStatus() != FriendRequestStatus.PENDING) {
            throw new BadRequestException("Friend request is not pending");
        }
        return friendRequest;
    }

    private FriendRequestResponse toFriendRequestResponse(FriendRequest friendRequest) {
        return friendMapper.toFriendRequestResponse(
                friendRequest,
                getUser(friendRequest.getRequesterId()),
                getUser(friendRequest.getReceiverId())
        );
    }

    private User getCurrentUser() {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        return getUser(principal.getId());
    }

    private User getUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private NormalizedPair normalizePair(String userId1, String userId2) {
        if (userId1.compareTo(userId2) <= 0) {
            return new NormalizedPair(userId1, userId2);
        }
        return new NormalizedPair(userId2, userId1);
    }

    private String otherUserId(Friendship friendship, String currentUserId) {
        return currentUserId.equals(friendship.getUserIdA()) ? friendship.getUserIdB() : friendship.getUserIdA();
    }

    private String displayName(User user) {
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            return user.getDisplayName();
        }
        return user.getUsername();
    }

    private User getFriendFromMap(Map<String, User> friendsById, String friendId) {
        User friend = friendsById.get(friendId);
        if (friend == null) {
            throw new UserNotFoundException("Friend user not found");
        }
        return friend;
    }

    private record NormalizedPair(String userIdA, String userIdB) {
    }
}
