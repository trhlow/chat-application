package com.chatrealtime.unit;

import com.chatrealtime.dto.request.UpdateUserProfileRequest;
import com.chatrealtime.dto.response.UserSearchResultResponse;
import com.chatrealtime.exception.BadRequestException;
import com.chatrealtime.dto.response.UserProfileResponse;
import com.chatrealtime.mapper.UserMapper;
import com.chatrealtime.domain.User;
import com.chatrealtime.repository.UserRepository;
import com.chatrealtime.storage.AvatarStorageService;
import com.chatrealtime.storage.AvatarUploadResult;
import com.chatrealtime.security.AuthContextService;
import com.chatrealtime.security.AuthUserPrincipal;
import com.chatrealtime.security.UserPrincipalService;
import com.chatrealtime.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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
    @Mock
    private UserPrincipalService userPrincipalService;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void getUsers_WhenQueryTooShort_ShouldRejectWithoutListingAllUsers() {
        assertThatThrownBy(() -> userService.getUsers(null))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> userService.getUsers(" a "))
                .isInstanceOf(BadRequestException.class);

        verify(userRepository, never()).findAll();
        verify(userRepository, never()).findByUsernameContainingIgnoreCase(any(String.class), any(Pageable.class));
    }

    @Test
    void getUsers_ShouldLimitSearchToTwentySortedResults() {
        User user = User.builder()
                .id("u1")
                .username("alice")
                .displayName("Alice")
                .avatar("https://res.cloudinary.com/demo/image/upload/avatar.png")
                .build();
        UserSearchResultResponse mapped = new UserSearchResultResponse(
                "u1",
                "alice",
                "Alice",
                "/api/users/u1/avatar",
                "/api/users/u1/avatar"
        );

        when(userRepository.findByUsernameContainingIgnoreCase(eq("al"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));
        when(userMapper.toSearchResult(user)).thenReturn(mapped);

        List<UserSearchResultResponse> response = userService.getUsers(" al ");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findByUsernameContainingIgnoreCase(eq("al"), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("username").isAscending()).isTrue();
        assertThat(response).containsExactly(mapped);
        assertThat(response.getFirst().avatarEndpoint()).isEqualTo("/api/users/u1/avatar");
    }

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
        verify(userPrincipalService).evictUserCaches("u1", "alice");
        verify(userPrincipalService).evictUserCaches("u1", "alice2");
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
        verify(userPrincipalService).evictUserCaches("u1", "alice");
        assertThat(captor.getValue().getAvatar()).isEqualTo("https://cdn.example/avatar.png");
        assertThat(captor.getValue().getAvatarPublicId()).isEqualTo("new-public-id");
        assertThat(captor.getValue().getAvatarProvider()).isEqualTo("cloudinary");
        assertThat(response.avatarEndpoint()).isEqualTo("/api/users/u1/avatar");
        assertThat(response.avatar()).isEqualTo("/api/users/u1/avatar");
    }

    @Test
    void uploadCurrentUserAvatar_WhenSaveFails_ShouldDeleteNewlyUploadedAvatar() {
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
                new byte[]{1, 2, 3}
        );

        when(authContextService.requireCurrentUser()).thenReturn(new AuthUserPrincipal("u1", "alice", "pw", 0));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(avatarStorageService.uploadAvatar("u1", file))
                .thenReturn(new AvatarUploadResult("https://cdn.example/avatar.png", "new-public-id", "cloudinary"));
        when(userRepository.save(any(User.class))).thenThrow(new IllegalStateException("db save failed"));

        assertThatThrownBy(() -> userService.uploadCurrentUserAvatar(file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("db save failed");

        verify(avatarStorageService).deleteAvatar("cloudinary", "new-public-id");
        verify(avatarStorageService, never()).deleteAvatar("local", "old.png");
        verify(userPrincipalService, never()).evictUserCaches(any(), any());
    }

    @Test
    void uploadCurrentUserAvatar_WhenSaveSucceedsButEvictFails_ShouldNotDeleteNewAvatar() {
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
                new byte[]{1, 2, 3}
        );

        when(authContextService.requireCurrentUser()).thenReturn(new AuthUserPrincipal("u1", "alice", "pw", 0));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(avatarStorageService.uploadAvatar("u1", file))
                .thenReturn(new AvatarUploadResult("https://cdn.example/avatar.png", "new-public-id", "cloudinary"));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new IllegalStateException("cache evict failed"))
                .when(userPrincipalService)
                .evictUserCaches(eq("u1"), eq("alice"));

        assertThatThrownBy(() -> userService.uploadCurrentUserAvatar(file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("cache evict failed");

        verify(avatarStorageService, never()).deleteAvatar(any(), any());
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
                user.getAvatar() == null ? null : "/api/users/" + user.getId() + "/avatar",
                user.getAvatar() == null ? null : "/api/users/" + user.getId() + "/avatar",
                user.isOnline(),
                user.getLastSeenAt()
        );
    }
}
