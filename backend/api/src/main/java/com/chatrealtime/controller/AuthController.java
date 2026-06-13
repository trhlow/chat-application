package com.chatrealtime.controller;

import com.chatrealtime.config.AuthCookieProperties;
import com.chatrealtime.dto.request.LoginRequest;
import com.chatrealtime.dto.request.LogoutRequest;
import com.chatrealtime.dto.request.RefreshTokenRequest;
import com.chatrealtime.dto.request.RegisterRequest;
import com.chatrealtime.dto.response.AuthResponse;
import jakarta.validation.Valid;
import com.chatrealtime.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final AuthCookieProperties authCookieProperties;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerUser(@Valid @RequestBody RegisterRequest request) {
        return withRefreshCookie(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return withRefreshCookie(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = "${app.auth.refresh-cookie.name:refreshToken}", required = false) String cookieRefreshToken,
            @Valid @RequestBody(required = false) RefreshTokenRequest request
    ) {
        return withRefreshCookie(authService.refresh(resolveRefreshTokenRequest(request, cookieRefreshToken)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "${app.auth.refresh-cookie.name:refreshToken}", required = false) String cookieRefreshToken,
            @RequestBody(required = false) LogoutRequest request
    ) {
        authService.logout(resolveLogoutRequest(request, cookieRefreshToken));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString())
                .build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll() {
        authService.logoutAll();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString())
                .build();
    }

    private ResponseEntity<AuthResponse> withRefreshCookie(AuthResponse response) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(response.refreshToken()).toString())
                .body(response);
    }

    private RefreshTokenRequest resolveRefreshTokenRequest(RefreshTokenRequest request, String cookieRefreshToken) {
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            return request;
        }
        return new RefreshTokenRequest(cookieRefreshToken);
    }

    private LogoutRequest resolveLogoutRequest(LogoutRequest request, String cookieRefreshToken) {
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            return request;
        }
        return new LogoutRequest(cookieRefreshToken);
    }

    private ResponseCookie refreshCookie(String refreshToken) {
        return ResponseCookie.from(authCookieProperties.name(), refreshToken)
                .httpOnly(true)
                .secure(authCookieProperties.secure())
                .sameSite(authCookieProperties.sameSite())
                .path(authCookieProperties.path())
                .maxAge(java.time.Duration.ofMillis(authService.refreshTokenTtlMs()))
                .build();
    }

    private ResponseCookie expiredRefreshCookie() {
        return ResponseCookie.from(authCookieProperties.name(), "")
                .httpOnly(true)
                .secure(authCookieProperties.secure())
                .sameSite(authCookieProperties.sameSite())
                .path(authCookieProperties.path())
                .maxAge(0)
                .build();
    }
}


