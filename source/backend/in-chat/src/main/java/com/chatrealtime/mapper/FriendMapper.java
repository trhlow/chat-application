package com.chatrealtime.mapper;

import com.chatrealtime.domain.FriendRequest;
import com.chatrealtime.domain.Friendship;
import com.chatrealtime.domain.User;
import com.chatrealtime.dto.response.FriendRequestResponse;
import com.chatrealtime.dto.response.FriendshipResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FriendMapper {
    private final UserMapper userMapper;

    public FriendRequestResponse toFriendRequestResponse(
            FriendRequest friendRequest,
            User requester,
            User receiver
    ) {
        return new FriendRequestResponse(
                friendRequest.getId(),
                userMapper.toUserProfileResponse(requester),
                userMapper.toUserProfileResponse(receiver),
                friendRequest.getStatus(),
                friendRequest.getCreatedAt(),
                friendRequest.getRespondedAt()
        );
    }

    public FriendshipResponse toFriendshipResponse(Friendship friendship, User friend) {
        return new FriendshipResponse(
                friendship.getId(),
                userMapper.toUserProfileResponse(friend),
                friendship.getCreatedAt()
        );
    }
}
