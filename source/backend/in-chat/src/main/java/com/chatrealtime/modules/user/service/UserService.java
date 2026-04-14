package com.chatrealtime.modules.user.service;

import com.chatrealtime.modules.user.dto.UpdateUserProfileRequest;
import com.chatrealtime.modules.user.dto.response.UserProfileResponse;
import com.chatrealtime.exception.ExistsUsernameException;
import com.chatrealtime.exception.UserNotFoundException;
import com.chatrealtime.modules.user.mapper.UserMapper;
import com.chatrealtime.modules.user.model.User;
import com.chatrealtime.modules.user.repository.UserRepository;
import com.chatrealtime.modules.user.storage.AvatarStorageService;
import com.chatrealtime.modules.user.storage.AvatarUploadResult;
import com.chatrealtime.security.AuthContextService;
import com.chatrealtime.security.AuthUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AuthContextService authContextService;
    private final AvatarStorageService avatarStorageService;

    public List<UserProfileResponse> getUsers(String query) {
        if (query == null || query.isBlank()) {
            return userRepository.findAll()
                    .stream()
                    .map(userMapper::toUserProfileResponse)
                    .toList();
        }
        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        return userRepository.findByUsernameContainingIgnoreCase(normalizedQuery)
                .stream()
                .map(userMapper::toUserProfileResponse)
                .toList();
    }

    public User getUserEntityById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    public UserProfileResponse getCurrentUserProfile() {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        return userMapper.toUserProfileResponse(getUserEntityById(principal.getId()));
    }

    public UserProfileResponse getUserById(String userId) {
        return userMapper.toUserProfileResponse(getUserEntityById(userId));
    }

    public UserProfileResponse updateCurrentUserProfile(UpdateUserProfileRequest request) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        User user = getUserEntityById(principal.getId());

        if (request.username() != null && !request.username().isBlank()) {
            String normalizedUsername = request.username().trim().toLowerCase(Locale.ROOT);
            if (!normalizedUsername.equals(user.getUsername()) && userRepository.existsByUsername(normalizedUsername)) {
                throw new ExistsUsernameException("Username already exists");
            }
            user.setUsername(normalizedUsername);
        }

        if (request.displayName() != null) {
            user.setDisplayName(trimToNull(request.displayName()));
        }
        if (request.bio() != null) {
            user.setBio(trimToNull(request.bio()));
        }
        if (request.phone() != null) {
            user.setPhone(trimToNull(request.phone()));
        }
        if (request.themePreference() != null) {
            user.setThemePreference(request.themePreference());
        }

        user.setUpdatedAt(Instant.now());
        return userMapper.toUserProfileResponse(userRepository.save(user));
    }

    public UserProfileResponse uploadCurrentUserAvatar(MultipartFile file) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        User user = getUserEntityById(principal.getId());

        String oldAvatarProvider = user.getAvatarProvider();
        String oldAvatarPublicId = user.getAvatarPublicId();
        AvatarUploadResult uploadedAvatar = avatarStorageService.uploadAvatar(user.getId(), file);

        user.setAvatar(uploadedAvatar.url());
        user.setAvatarPublicId(uploadedAvatar.publicId());
        user.setAvatarProvider(uploadedAvatar.provider());
        user.setUpdatedAt(Instant.now());

        User savedUser = userRepository.save(user);
        avatarStorageService.deleteAvatar(oldAvatarProvider, oldAvatarPublicId);
        return userMapper.toUserProfileResponse(savedUser);
    }

    private String trimToNull(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}



