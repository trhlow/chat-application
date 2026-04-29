package com.chatrealtime.service;

import com.chatrealtime.repository.FriendshipRepository;
import com.chatrealtime.repository.RoomRepository;
import com.chatrealtime.util.UserIdPair;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Controls who may load another user's avatar bytes or Cloudinary redirect.
 */
@Service
@RequiredArgsConstructor
public class UserAvatarAccessPolicy {
    private final FriendshipRepository friendshipRepository;
    private final RoomRepository roomRepository;

    public void assertCanViewUserAvatar(String actorUserId, String targetUserId) {
        if (actorUserId.equals(targetUserId)) {
            return;
        }
        UserIdPair.Ordered pair = UserIdPair.order(actorUserId, targetUserId);
        if (friendshipRepository.existsByUserIdAAndUserIdB(pair.userIdA(), pair.userIdB())) {
            return;
        }
        if (roomRepository.existsRoomSharedBy(actorUserId, targetUserId)) {
            return;
        }
        throw new AccessDeniedException("Forbidden");
    }
}
