package com.chatrealtime.service.impl;

import com.chatrealtime.service.UserService;

import com.chatrealtime.dto.request.UpdateUserProfileRequest;
import com.chatrealtime.dto.response.UserProfileResponse;
import com.chatrealtime.exception.ExistsUsernameException;
import com.chatrealtime.exception.UserNotFoundException;
import com.chatrealtime.mapper.UserMapper;
import com.chatrealtime.domain.User;
import com.chatrealtime.repository.UserRepository;
import com.chatrealtime.storage.AvatarStorageService;
import com.chatrealtime.storage.AvatarUploadResult;
import com.chatrealtime.security.AuthContextService;
import com.chatrealtime.security.AuthUserPrincipal;
import com.chatrealtime.security.UserPrincipalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AuthContextService authContextService;
    private final AvatarStorageService avatarStorageService;
    private final UserPrincipalService userPrincipalService;

    @Override
    public List<UserProfileResponse> getUsers(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        return userRepository.findByUsernameContainingIgnoreCase(normalizedQuery)
                .stream()
                .map(userMapper::toUserProfileResponse)
                .toList();
    }

    @Override
    public User getUserEntityById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    @Override
    public UserProfileResponse getCurrentUserProfile() {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        return userMapper.toUserProfileResponse(getUserEntityById(principal.getId()));
    }

    @Override
    public UserProfileResponse getUserById(String userId) {
        return userMapper.toUserProfileResponse(getUserEntityById(userId));
    }

    @Override
    public UserProfileResponse updateCurrentUserProfile(UpdateUserProfileRequest request) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        User user = getUserEntityById(principal.getId());
        String previousUsername = user.getUsername();

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
        User savedUser = userRepository.save(user);
        userPrincipalService.evictUserCaches(savedUser.getId(), previousUsername);
        userPrincipalService.evictUserCaches(savedUser.getId(), savedUser.getUsername());
        return userMapper.toUserProfileResponse(savedUser);
    }

    @Override
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
        userPrincipalService.evictUserCaches(savedUser.getId(), savedUser.getUsername());
        avatarStorageService.deleteAvatar(oldAvatarProvider, oldAvatarPublicId);
        return userMapper.toUserProfileResponse(savedUser);
    }

    private String trimToNull(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}



