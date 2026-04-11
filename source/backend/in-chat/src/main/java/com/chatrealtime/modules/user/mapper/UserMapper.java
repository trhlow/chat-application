package com.chatrealtime.modules.user.mapper;

import com.chatrealtime.modules.user.dto.response.UserProfileResponse;
import com.chatrealtime.modules.user.model.User;
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


