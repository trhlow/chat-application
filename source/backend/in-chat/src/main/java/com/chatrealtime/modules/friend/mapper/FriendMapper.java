package com.chatrealtime.modules.friend.mapper;

import com.chatrealtime.modules.friend.dto.response.FriendRequestResponse;
import com.chatrealtime.modules.friend.dto.response.FriendshipResponse;
import com.chatrealtime.modules.friend.model.FriendRequest;
import com.chatrealtime.modules.friend.model.Friendship;
import com.chatrealtime.modules.user.mapper.UserMapper;
import com.chatrealtime.modules.user.model.User;
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
