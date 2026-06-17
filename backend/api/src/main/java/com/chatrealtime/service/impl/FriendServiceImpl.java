package com.chatrealtime.service.impl;

import com.chatrealtime.domain.FriendRequest;
import com.chatrealtime.domain.FriendRequestStatus;
import com.chatrealtime.domain.Friendship;
import com.chatrealtime.domain.User;
import com.chatrealtime.domain.UserBlock;
import com.chatrealtime.dto.request.CreateFriendRequestRequest;
import com.chatrealtime.dto.response.FriendRequestResponse;
import com.chatrealtime.dto.response.FriendUserResponse;
import com.chatrealtime.dto.response.FriendshipResponse;
import com.chatrealtime.exception.BadRequestException;
import com.chatrealtime.exception.ConflictException;
import com.chatrealtime.exception.FriendRequestNotFoundException;
import com.chatrealtime.exception.FriendshipNotFoundException;
import com.chatrealtime.exception.UserNotFoundException;
import com.chatrealtime.mapper.FriendMapper;
import com.chatrealtime.repository.FriendRequestRepository;
import com.chatrealtime.repository.FriendshipRepository;
import com.chatrealtime.repository.UserBlockRepository;
import com.chatrealtime.repository.UserRepository;
import com.chatrealtime.security.AuthContextService;
import com.chatrealtime.security.AuthUserPrincipal;
import com.chatrealtime.service.FriendService;
import com.chatrealtime.service.NotificationService;
import com.chatrealtime.util.UserIdPair;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {
    private static final String NOTIFICATION_FRIEND_REQUEST = "friend_request";
    private static final String NOTIFICATION_FRIEND_ACCEPTED = "friend_request_accepted";

    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final UserBlockRepository userBlockRepository;
    private final FriendMapper friendMapper;
    private final AuthContextService authContextService;
    private final NotificationService notificationService;

    @Override
    public FriendRequestResponse sendFriendRequest(CreateFriendRequestRequest request) {
        User requester = getCurrentUser();
        User receiver = userRepository.findById(request.receiverId())
                .orElseThrow(() -> new UserNotFoundException("Receiver not found"));

        if (requester.getId().equals(receiver.getId())) {
            throw new BadRequestException("Cannot send a friend request to yourself");
        }
        ensureNoBlockBetween(requester.getId(), receiver.getId());

        UserIdPair.Ordered pair = UserIdPair.order(requester.getId(), receiver.getId());
        if (friendshipRepository.existsByUserIdAAndUserIdB(pair.userIdA(), pair.userIdB())) {
            throw new BadRequestException("Users are already friends");
        }

        boolean hasPendingRequest = friendRequestRepository.existsByUserIdAAndUserIdBAndStatus(
                pair.userIdA(),
                pair.userIdB(),
                FriendRequestStatus.PENDING
        );
        if (hasPendingRequest) {
            throw new BadRequestException("A pending friend request already exists");
        }

        FriendRequest savedRequest;
        try {
            savedRequest = friendRequestRepository.save(FriendRequest.builder()
                    .requesterId(requester.getId())
                    .receiverId(receiver.getId())
                    .userIdA(pair.userIdA())
                    .userIdB(pair.userIdB())
                    .status(FriendRequestStatus.PENDING)
                    .createdAt(Instant.now())
                    .build());
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("A pending friend request already exists");
        }

        notificationService.createSystemNotification(
                receiver.getId(),
                NOTIFICATION_FRIEND_REQUEST,
                "New friend request",
                displayName(requester) + " sent you a friend request",
                savedRequest.getId()
        );

        return friendMapper.toFriendRequestResponse(savedRequest, requester, receiver);
    }

    @Override
    public List<FriendRequestResponse> getIncomingRequests() {
        User currentUser = getCurrentUser();
        List<FriendRequest> requests = friendRequestRepository.findByReceiverIdAndStatusOrderByCreatedAtDesc(
                currentUser.getId(),
                FriendRequestStatus.PENDING
        );
        return mapToFriendRequestResponses(requests);
    }

    @Override
    public List<FriendRequestResponse> getOutgoingRequests() {
        User currentUser = getCurrentUser();
        List<FriendRequest> requests = friendRequestRepository.findByRequesterIdAndStatusOrderByCreatedAtDesc(
                currentUser.getId(),
                FriendRequestStatus.PENDING
        );
        return mapToFriendRequestResponses(requests);
    }

    private List<FriendRequestResponse> mapToFriendRequestResponses(List<FriendRequest> requests) {
        if (requests.isEmpty()) {
            return List.of();
        }
        java.util.Set<String> userIds = new java.util.HashSet<>();
        requests.forEach(req -> {
            userIds.add(req.getRequesterId());
            userIds.add(req.getReceiverId());
        });
        Map<String, User> usersById = userRepository.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return requests.stream()
                .map(req -> friendMapper.toFriendRequestResponse(
                        req,
                        usersById.get(req.getRequesterId()),
                        usersById.get(req.getReceiverId())
                ))
                .toList();
    }

    @Override
    public FriendRequestResponse acceptRequest(String requestId) {
        User currentUser = getCurrentUser();
        FriendRequest friendRequest = getPendingRequest(requestId);

        if (!currentUser.getId().equals(friendRequest.getReceiverId())) {
            throw new AccessDeniedException("Forbidden");
        }

        User requester = getUser(friendRequest.getRequesterId());
        ensureNoBlockBetween(requester.getId(), currentUser.getId());
        UserIdPair.Ordered pair = UserIdPair.order(friendRequest.getRequesterId(), friendRequest.getReceiverId());
        if (!friendshipRepository.existsByUserIdAAndUserIdB(pair.userIdA(), pair.userIdB())) {
            try {
                friendshipRepository.save(Friendship.builder()
                        .userIdA(pair.userIdA())
                        .userIdB(pair.userIdB())
                        .userIds(List.of(pair.userIdA(), pair.userIdB()))
                        .createdAt(Instant.now())
                        .build());
            } catch (DuplicateKeyException ignored) {
                // Another request accepted the same canonical pair concurrently.
            }
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

    @Override
    public FriendRequestResponse rejectRequest(String requestId) {
        User currentUser = getCurrentUser();
        FriendRequest friendRequest = getPendingRequest(requestId);

        if (!currentUser.getId().equals(friendRequest.getReceiverId())) {
            throw new AccessDeniedException("Forbidden");
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

    @Override
    public FriendRequestResponse cancelRequest(String requestId) {
        User currentUser = getCurrentUser();
        FriendRequest friendRequest = getPendingRequest(requestId);

        if (!currentUser.getId().equals(friendRequest.getRequesterId())) {
            throw new AccessDeniedException("Forbidden");
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

    @Override
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

    @Override
    public void removeFriend(String friendId) {
        User currentUser = getCurrentUser();
        getUser(friendId);
        UserIdPair.Ordered pair = UserIdPair.order(currentUser.getId(), friendId);
        Friendship friendship = friendshipRepository.findByUserIdAAndUserIdB(pair.userIdA(), pair.userIdB())
                .orElseThrow(() -> new FriendshipNotFoundException("Friendship not found"));
        friendshipRepository.delete(friendship);
    }

    @Override
    public FriendUserResponse blockUser(String userId) {
        User currentUser = getCurrentUser();
        User blockedUser = getUser(userId);
        if (currentUser.getId().equals(blockedUser.getId())) {
            throw new BadRequestException("Cannot block yourself");
        }

        if (!userBlockRepository.existsByBlockerIdAndBlockedId(currentUser.getId(), blockedUser.getId())) {
            try {
                userBlockRepository.save(UserBlock.builder()
                        .blockerId(currentUser.getId())
                        .blockedId(blockedUser.getId())
                        .createdAt(Instant.now())
                        .build());
            } catch (DuplicateKeyException ignored) {
                // Concurrent request created the same block.
            }
        }

        removeFriendIfExists(currentUser.getId(), blockedUser.getId());
        return friendMapper.toFriendUserResponse(blockedUser);
    }

    @Override
    public void unblockUser(String userId) {
        User currentUser = getCurrentUser();
        getUser(userId);
        userBlockRepository.findByBlockerIdAndBlockedId(currentUser.getId(), userId)
                .ifPresent(userBlockRepository::delete);
    }

    @Override
    public List<FriendUserResponse> getBlockedUsers() {
        User currentUser = getCurrentUser();
        List<String> blockedIds = userBlockRepository.findByBlockerIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(UserBlock::getBlockedId)
                .toList();
        Map<String, User> usersById = userRepository.findAllById(blockedIds)
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return blockedIds.stream()
                .map(usersById::get)
                .filter(java.util.Objects::nonNull)
                .map(friendMapper::toFriendUserResponse)
                .toList();
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

    private void ensureNoBlockBetween(String userIdA, String userIdB) {
        if (userBlockRepository.existsBetweenUsers(userIdA, userIdB)) {
            throw new BadRequestException("Users cannot interact because one user has blocked the other");
        }
    }

    private void removeFriendIfExists(String userIdA, String userIdB) {
        UserIdPair.Ordered pair = UserIdPair.order(userIdA, userIdB);
        friendshipRepository.findByUserIdAAndUserIdB(pair.userIdA(), pair.userIdB())
                .ifPresent(friendshipRepository::delete);
    }
}
