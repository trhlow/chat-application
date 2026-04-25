package com.chatrealtime.security;

import com.chatrealtime.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {
    private final JwtTokenService jwtTokenService;
    private final UserPrincipalService userPrincipalService;
    private final WebSocketAuthorizationService webSocketAuthorizationService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (accessor.getCommand() == StompCommand.CONNECT) {
            accessor.setUser(authenticate(accessor));
            return message;
        }

        if (accessor.getCommand() == StompCommand.SEND || accessor.getCommand() == StompCommand.SUBSCRIBE) {
            AuthUserPrincipal principal = requirePrincipal(accessor);
            webSocketAuthorizationService.authorize(principal, accessor.getCommand(), requireDestination(accessor));
        }
        return message;
    }

    private UsernamePasswordAuthenticationToken authenticate(StompHeaderAccessor accessor) {
        String authorizationHeader = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new InvalidCredentialsException("Unauthorized");
        }

        String token = authorizationHeader.substring(7).trim();
        if (!jwtTokenService.isTokenValid(token)) {
            throw new InvalidCredentialsException("Unauthorized");
        }

        String userId = jwtTokenService.extractUserId(token);
        int tokenVersion = jwtTokenService.extractTokenVersion(token);
        AuthUserPrincipal principal = loadActivePrincipal(userId, tokenVersion);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private AuthUserPrincipal requirePrincipal(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof UsernamePasswordAuthenticationToken authentication)
                || !(authentication.getPrincipal() instanceof AuthUserPrincipal principal)) {
            throw new InvalidCredentialsException("Unauthorized");
        }
        return loadActivePrincipal(principal.getId(), principal.getTokenVersion());
    }

    private AuthUserPrincipal loadActivePrincipal(String userId, int expectedTokenVersion) {
        AuthUserPrincipal principal;
        try {
            principal = userPrincipalService.loadByUserId(userId);
        } catch (RuntimeException exception) {
            throw new InvalidCredentialsException("Unauthorized");
        }
        if (principal.getTokenVersion() != expectedTokenVersion) {
            throw new InvalidCredentialsException("Unauthorized");
        }
        return principal;
    }

    private String requireDestination(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || destination.isBlank()) {
            throw new InvalidCredentialsException("Unauthorized");
        }
        return destination;
    }
}

