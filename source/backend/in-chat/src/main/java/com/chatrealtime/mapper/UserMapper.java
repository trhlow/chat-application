package com.chatrealtime.mapper;

import com.chatrealtime.dto.response.UserProfileResponse;
import com.chatrealtime.domain.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserProfileResponse toUserProfileResponse(User user);
}


