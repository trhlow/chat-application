package com.chatrealtime.controller;

import com.chatrealtime.dto.request.UpdateUserProfileRequest;
import com.chatrealtime.dto.response.PublicUserProfileResponse;
import com.chatrealtime.dto.response.UserProfileResponse;
import com.chatrealtime.dto.response.UserSearchResultResponse;
import com.chatrealtime.security.AuthContextService;
import com.chatrealtime.service.UserAvatarAccessPolicy;
import com.chatrealtime.service.UserAvatarDownloadService;
import com.chatrealtime.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final AuthContextService authContextService;
    private final UserAvatarDownloadService userAvatarDownloadService;
    private final UserAvatarAccessPolicy userAvatarAccessPolicy;

    @GetMapping
    public List<UserSearchResultResponse> getUsers(@RequestParam(required = false) String query) {
        return userService.getUsers(query);
    }

    @GetMapping("/me")
    public UserProfileResponse getMe() {
        return userService.getCurrentUserProfile();
    }

    @GetMapping("/{userId}/avatar")
    public ResponseEntity<?> getUserAvatar(@PathVariable String userId) {
        var principal = authContextService.requireCurrentUser();
        userAvatarAccessPolicy.assertCanViewUserAvatar(principal.getId(), userId);
        UserAvatarDownloadService.AvatarDownloadResult result = userAvatarDownloadService.resolve(userId);
        if (result instanceof UserAvatarDownloadService.AvatarDownloadResult.Redirect redirect) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(redirect.location())
                    .build();
        }
        if (result instanceof UserAvatarDownloadService.AvatarDownloadResult.File file) {
            return ResponseEntity.ok()
                    .contentType(file.mediaType())
                    .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
                    .body(file.body());
        }
        throw new IllegalStateException("Unexpected avatar result");
    }

    @GetMapping("/{userId}")
    public PublicUserProfileResponse getUser(@PathVariable String userId) {
        return userService.getPublicUserProfileById(userId);
    }

    @PutMapping("/me")
    public UserProfileResponse updateMe(@Valid @RequestBody UpdateUserProfileRequest request) {
        return userService.updateCurrentUserProfile(request);
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserProfileResponse uploadMyAvatar(@RequestParam("file") MultipartFile file) {
        return userService.uploadCurrentUserAvatar(file);
    }
}
