package com.chatrealtime.modules.user.controller;

import com.chatrealtime.modules.user.dto.UpdateUserProfileRequest;
import com.chatrealtime.modules.user.dto.response.UserProfileResponse;
import jakarta.validation.Valid;
import com.chatrealtime.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public List<UserProfileResponse> getUsers(@RequestParam(required = false) String query) {
        return userService.getUsers(query);
    }

    @GetMapping("/me")
    public UserProfileResponse getMe() {
        return userService.getCurrentUserProfile();
    }

    @GetMapping("/{userId}")
    public UserProfileResponse getUser(@PathVariable String userId) {
        return userService.getUserById(userId);
    }

    @PutMapping("/me")
    public UserProfileResponse updateMe(@Valid @RequestBody UpdateUserProfileRequest request) {
        return userService.updateCurrentUserProfile(request);
    }
}



