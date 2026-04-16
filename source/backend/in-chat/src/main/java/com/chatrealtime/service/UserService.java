package com.chatrealtime.service;

import com.chatrealtime.domain.User;
import com.chatrealtime.dto.request.UpdateUserProfileRequest;
import com.chatrealtime.dto.response.UserProfileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {
    List<UserProfileResponse> getUsers(String query);

    User getUserEntityById(String userId);

    UserProfileResponse getCurrentUserProfile();

    UserProfileResponse getUserById(String userId);

    UserProfileResponse updateCurrentUserProfile(UpdateUserProfileRequest request);

    UserProfileResponse uploadCurrentUserAvatar(MultipartFile file);
}
