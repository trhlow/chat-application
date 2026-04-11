package com.chatrealtime.mapper;

import com.chatrealtime.dto.user.response.UserProfileResponse;
import com.chatrealtime.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserProfileResponse toUserProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getAvatar(),
                user.isOnline(),
                user.getLastSeenAt()
        );
    }
}
