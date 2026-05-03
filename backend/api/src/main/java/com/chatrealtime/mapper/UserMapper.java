package com.chatrealtime.mapper;

import com.chatrealtime.dto.response.PublicUserProfileResponse;
import com.chatrealtime.dto.response.FriendUserResponse;
import com.chatrealtime.dto.response.UserProfileResponse;
import com.chatrealtime.dto.response.UserSearchResultResponse;
import com.chatrealtime.domain.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    default UserProfileResponse toUserProfileResponse(User user) {
        if (user == null) {
            return null;
        }

        String avatarEndpoint = avatarEndpoint(user);
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getBio(),
                user.getPhone(),
                user.getThemePreference(),
                avatarEndpoint,
                avatarEndpoint,
                user.isOnline(),
                user.getLastSeenAt()
        );
    }

    default PublicUserProfileResponse toPublicUserProfileResponse(User user) {
        if (user == null) {
            return null;
        }

        String avatarEndpoint = avatarEndpoint(user);
        return new PublicUserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                avatarEndpoint,
                avatarEndpoint
        );
    }

    default UserSearchResultResponse toSearchResult(User user) {
        if (user == null) {
            return null;
        }

        String avatarEndpoint = avatarEndpoint(user);
        return new UserSearchResultResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                avatarEndpoint,
                avatarEndpoint
        );
    }

    default FriendUserResponse toFriendUserResponse(User user) {
        if (user == null) {
            return null;
        }

        String avatarEndpoint = avatarEndpoint(user);
        return new FriendUserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                avatarEndpoint,
                avatarEndpoint
        );
    }

    default String avatarEndpoint(User user) {
        return user.getAvatar() == null || user.getAvatar().isBlank()
                ? null
                : "/api/users/" + user.getId() + "/avatar";
    }
}


