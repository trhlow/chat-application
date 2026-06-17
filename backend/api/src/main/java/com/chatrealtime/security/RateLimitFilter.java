package com.chatrealtime.security;

import com.chatrealtime.config.AppRateLimitProperties;
import com.chatrealtime.dto.response.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final String RETRY_AFTER_SECONDS = "60";

    private final AppRateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final RateLimitCounterStore counterStore;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        RateLimitRule rule = resolveRule(request);
        if (rule == null || !properties.isEnabled() || rule.maxRequests() <= 0) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = normalizePrefix(properties.getKeyPrefix()) + ':' + rule.name() + ':' + clientKey(request);
        if (counterStore.incrementAndGet(key, WINDOW) <= rule.maxRequests()) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiErrorResponse error = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "TOO_MANY_REQUESTS",
                "Too many requests",
                request.getRequestURI(),
                null
        );
        objectMapper.writeValue(response.getWriter(), error);
    }

    private RateLimitRule resolveRule(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        if (HttpMethod.POST.matches(method) && "/api/auth/login".equals(path)) {
            return new RateLimitRule("auth-login", properties.getLoginLimitPerMinute());
        }
        if (HttpMethod.POST.matches(method) && "/api/auth/register".equals(path)) {
            return new RateLimitRule("auth-register", properties.getRegisterLimitPerMinute());
        }
        if (HttpMethod.POST.matches(method) && "/api/auth/refresh".equals(path)) {
            return new RateLimitRule("auth-refresh", properties.getRefreshLimitPerMinute());
        }
        if (HttpMethod.POST.matches(method) && "/api/messages/attachments".equals(path)) {
            return new RateLimitRule("message-upload", properties.getUploadLimitPerMinute());
        }
        if (path.equals("/ws") || path.startsWith("/ws/")) {
            return new RateLimitRule("websocket", properties.getWebsocketLimitPerMinute());
        }

        return null;
    }

    private String clientKey(HttpServletRequest request) {
        String forwardedFor = properties.isTrustForwardedHeaders()
                ? request.getHeader("X-Forwarded-For")
                : null;
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String[] parts = forwardedFor.split(",");
            return parts[parts.length - 1].trim();
        }
        return request.getRemoteAddr();
    }

    private String normalizePrefix(String prefix) {
        return prefix == null || prefix.isBlank() ? "in-chat:rate-limit" : prefix.trim();
    }

    private record RateLimitRule(String name, int maxRequests) {
    }
}
