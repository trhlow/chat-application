package com.chatrealtime.service;

import com.chatrealtime.domain.User;
import com.chatrealtime.dto.request.UpdateUserProfileRequest;
import com.chatrealtime.dto.response.PublicUserProfileResponse;
import com.chatrealtime.dto.response.UserProfileResponse;
import com.chatrealtime.dto.response.UserSearchResultResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {
    List<UserSearchResultResponse> getUsers(String query);

    User getUserEntityById(String userId);

    UserProfileResponse getCurrentUserProfile();

    PublicUserProfileResponse getPublicUserProfileById(String userId);

    UserProfileResponse updateCurrentUserProfile(UpdateUserProfileRequest request);

    UserProfileResponse uploadCurrentUserAvatar(MultipartFile file);
}
