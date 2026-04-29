package com.chatrealtime.mapper;

import com.chatrealtime.dto.response.PublicUserProfileResponse;
import com.chatrealtime.dto.response.UserProfileResponse;
import com.chatrealtime.dto.response.UserSearchResultResponse;
import com.chatrealtime.domain.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserProfileResponse toUserProfileResponse(User user);

    @Mapping(target = "online", expression = "java(user.isOnline())")
    PublicUserProfileResponse toPublicUserProfileResponse(User user);

    @Mapping(target = "avatarEndpoint", expression = "java(\"/api/users/\" + user.getId() + \"/avatar\")")
    UserSearchResultResponse toSearchResult(User user);
}


