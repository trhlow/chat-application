package com.chatrealtime.event;

import com.chatrealtime.security.AuthUserPrincipal;
import com.chatrealtime.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
public class PresenceWebSocketEventListener {
    private final PresenceService presenceService;

    @EventListener
    public void onConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        AuthUserPrincipal principal = extractPrincipal(accessor);
        if (principal != null) {
            presenceService.markOnline(principal.getId());
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        AuthUserPrincipal principal = extractPrincipal(accessor);
        if (principal != null) {
            presenceService.markOffline(principal.getId());
        }
    }

    private AuthUserPrincipal extractPrincipal(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof UsernamePasswordAuthenticationToken token
                && token.getPrincipal() instanceof AuthUserPrincipal principal) {
            return principal;
        }
        return null;
    }
}

