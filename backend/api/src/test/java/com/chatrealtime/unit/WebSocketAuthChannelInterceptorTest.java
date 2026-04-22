package com.chatrealtime.unit;

import com.chatrealtime.security.AuthUserPrincipal;
import com.chatrealtime.security.JwtTokenService;
import com.chatrealtime.security.UserPrincipalService;
import com.chatrealtime.security.WebSocketAuthChannelInterceptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthChannelInterceptorTest {
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private UserPrincipalService userPrincipalService;

    @Test
    void preSend_ShouldAuthenticateConnectWhenTokenVersionMatches() {
        WebSocketAuthChannelInterceptor interceptor = new WebSocketAuthChannelInterceptor(
                jwtTokenService,
                userPrincipalService
        );
        AuthUserPrincipal principal = new AuthUserPrincipal("u1", "alice", "pw", 3);

        when(jwtTokenService.isTokenValid("token")).thenReturn(true);
        when(jwtTokenService.extractUserId("token")).thenReturn("u1");
        when(jwtTokenService.extractTokenVersion("token")).thenReturn(3);
        when(userPrincipalService.loadByUserId("u1")).thenReturn(principal);

        Message<?> result = interceptor.preSend(connectMessage("token"), mock(MessageChannel.class));

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
        assertThat(accessor.getUser()).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(((UsernamePasswordAuthenticationToken) accessor.getUser()).getPrincipal()).isEqualTo(principal);
    }

    @Test
    void preSend_ShouldIgnoreConnectWhenTokenVersionIsStale() {
        WebSocketAuthChannelInterceptor interceptor = new WebSocketAuthChannelInterceptor(
                jwtTokenService,
                userPrincipalService
        );
        AuthUserPrincipal principal = new AuthUserPrincipal("u1", "alice", "pw", 4);

        when(jwtTokenService.isTokenValid("token")).thenReturn(true);
        when(jwtTokenService.extractUserId("token")).thenReturn("u1");
        when(jwtTokenService.extractTokenVersion("token")).thenReturn(3);
        when(userPrincipalService.loadByUserId("u1")).thenReturn(principal);

        Message<?> result = interceptor.preSend(connectMessage("token"), mock(MessageChannel.class));

        assertThat(StompHeaderAccessor.wrap(result).getUser()).isNull();
    }

    private Message<byte[]> connectMessage(String token) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        accessor.setNativeHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
