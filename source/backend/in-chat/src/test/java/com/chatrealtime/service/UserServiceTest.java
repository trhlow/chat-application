package com.chatrealtime.modules.user.service;

import com.chatrealtime.modules.user.dto.UpdateUserProfileRequest;
import com.chatrealtime.modules.user.dto.response.UserProfileResponse;
import com.chatrealtime.modules.user.mapper.UserMapper;
import com.chatrealtime.modules.user.model.User;
import com.chatrealtime.modules.user.repository.UserRepository;
import com.chatrealtime.modules.user.storage.AvatarStorageService;
import com.chatrealtime.modules.user.storage.AvatarUploadResult;
import com.chatrealtime.security.AuthContextService;
import com.chatrealtime.security.AuthUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private AuthContextService authContextService;
    @Mock
    private AvatarStorageService avatarStorageService;

    @InjectMocks
    private UserService userService;

    @Test
    void updateCurrentUserProfile_ShouldUpdateProfileFields() {
        User user = User.builder()
                .id("u1")
                .username("alice")
                .email("alice@example.com")
                .themePreference("system")
                .build();
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "alice2",
                "Alice Liddell",
                "Chatting from Wonderland",
                "+84901234567",
                "dark"
        );

        when(authContextService.requireCurrentUser()).thenReturn(new AuthUserPrincipal("u1", "alice", "pw", 0));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("alice2")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toUserProfileResponse(any(User.class))).thenAnswer(invocation -> toResponse(invocation.getArgument(0)));

        UserProfileResponse response = userService.updateCurrentUserProfile(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("alice2");
        assertThat(captor.getValue().getDisplayName()).isEqualTo("Alice Liddell");
        assertThat(captor.getValue().getBio()).isEqualTo("Chatting from Wonderland");
        assertThat(captor.getValue().getPhone()).isEqualTo("+84901234567");
        assertThat(captor.getValue().getThemePreference()).isEqualTo("dark");
        assertThat(response.displayName()).isEqualTo("Alice Liddell");
    }

    @Test
    void uploadCurrentUserAvatar_ShouldReplaceAvatarAndDeleteOldOne() {
        User user = User.builder()
                .id("u1")
                .username("alice")
                .email("alice@example.com")
                .avatar("http://localhost:8080/uploads/avatars/old.png")
                .avatarPublicId("old.png")
                .avatarProvider("local")
                .build();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[] {1, 2, 3}
        );

        when(authContextService.requireCurrentUser()).thenReturn(new AuthUserPrincipal("u1", "alice", "pw", 0));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(avatarStorageService.uploadAvatar("u1", file))
                .thenReturn(new AvatarUploadResult("https://cdn.example/avatar.png", "new-public-id", "cloudinary"));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toUserProfileResponse(any(User.class))).thenAnswer(invocation -> toResponse(invocation.getArgument(0)));

        UserProfileResponse response = userService.uploadCurrentUserAvatar(file);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        verify(avatarStorageService).deleteAvatar("local", "old.png");
        assertThat(captor.getValue().getAvatar()).isEqualTo("https://cdn.example/avatar.png");
        assertThat(captor.getValue().getAvatarPublicId()).isEqualTo("new-public-id");
        assertThat(captor.getValue().getAvatarProvider()).isEqualTo("cloudinary");
        assertThat(response.avatar()).isEqualTo("https://cdn.example/avatar.png");
    }

    private UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getBio(),
                user.getPhone(),
                user.getThemePreference(),
                user.getAvatar(),
                user.getAvatarProvider(),
                user.isOnline(),
                user.getLastSeenAt()
        );
    }
}
