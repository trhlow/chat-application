package com.chatrealtime.unit;

import com.chatrealtime.security.AuthUserPrincipal;
import com.chatrealtime.security.JwtTokenService;
import com.chatrealtime.security.UserPrincipalService;
import com.chatrealtime.security.WebSocketAuthChannelInterceptor;
import com.chatrealtime.security.WebSocketAuthorizationService;
import io.jsonwebtoken.Claims;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthChannelInterceptorTest {
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private UserPrincipalService userPrincipalService;
    @Mock
    private WebSocketAuthorizationService webSocketAuthorizationService;

    @Test
    void preSend_ShouldAuthenticateConnectWhenTokenVersionMatches() {
        WebSocketAuthChannelInterceptor interceptor = new WebSocketAuthChannelInterceptor(
                jwtTokenService,
                userPrincipalService,
                webSocketAuthorizationService
        );
        AuthUserPrincipal principal = new AuthUserPrincipal("u1", "alice", "pw", 3);

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("u1");
        when(claims.get("tokenVersion")).thenReturn(3);
        when(jwtTokenService.parseValidClaims("token")).thenReturn(Optional.of(claims));
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
                userPrincipalService,
                webSocketAuthorizationService
        );
        AuthUserPrincipal principal = new AuthUserPrincipal("u1", "alice", "pw", 4);

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("u1");
        when(claims.get("tokenVersion")).thenReturn(3);
        when(jwtTokenService.parseValidClaims("token")).thenReturn(Optional.of(claims));
        when(userPrincipalService.loadByUserId("u1")).thenReturn(principal);

        assertThatThrownBy(() -> interceptor.preSend(connectMessage("token"), mock(MessageChannel.class)))
                .isInstanceOf(com.chatrealtime.exception.InvalidCredentialsException.class);
    }

    @Test
    void preSend_ShouldRejectConnectWithoutBearerToken() {
        WebSocketAuthChannelInterceptor interceptor = new WebSocketAuthChannelInterceptor(
                jwtTokenService,
                userPrincipalService,
                webSocketAuthorizationService
        );

        assertThatThrownBy(() -> interceptor.preSend(connectMessageWithoutAuthorization(), mock(MessageChannel.class)))
                .isInstanceOf(com.chatrealtime.exception.InvalidCredentialsException.class);
    }

    @Test
    void preSend_ShouldAuthorizeSubscribeDestinationForAuthenticatedPrincipal() {
        WebSocketAuthChannelInterceptor interceptor = new WebSocketAuthChannelInterceptor(
                jwtTokenService,
                userPrincipalService,
                webSocketAuthorizationService
        );
        AuthUserPrincipal principal = new AuthUserPrincipal("u1", "alice", "pw", 3);
        when(userPrincipalService.loadByUserId("u1")).thenReturn(principal);

        Message<?> result = interceptor.preSend(
                subscribeMessage(principal, "/topic/rooms/room-1/messages"),
                mock(MessageChannel.class)
        );

        assertThat(result).isNotNull();
        verify(webSocketAuthorizationService).authorize(principal, StompCommand.SUBSCRIBE, "/topic/rooms/room-1/messages");
    }

    @Test
    void preSend_ShouldRejectSendWithoutAuthenticatedPrincipal() {
        WebSocketAuthChannelInterceptor interceptor = new WebSocketAuthChannelInterceptor(
                jwtTokenService,
                userPrincipalService,
                webSocketAuthorizationService
        );

        assertThatThrownBy(() -> interceptor.preSend(
                sendMessageWithoutUser("/app/rooms/room-1/messages"),
                mock(MessageChannel.class)
        )).isInstanceOf(com.chatrealtime.exception.InvalidCredentialsException.class);
    }

    @Test
    void preSend_ShouldPropagateAuthorizationFailures() {
        WebSocketAuthChannelInterceptor interceptor = new WebSocketAuthChannelInterceptor(
                jwtTokenService,
                userPrincipalService,
                webSocketAuthorizationService
        );
        AuthUserPrincipal principal = new AuthUserPrincipal("u1", "alice", "pw", 3);
        when(userPrincipalService.loadByUserId("u1")).thenReturn(principal);
        org.mockito.Mockito.doThrow(new AccessDeniedException("Forbidden"))
                .when(webSocketAuthorizationService)
                .authorize(principal, StompCommand.SEND, "/app/rooms/room-1/messages");

        assertThatThrownBy(() -> interceptor.preSend(
                sendMessage(principal, "/app/rooms/room-1/messages"),
                mock(MessageChannel.class)
        )).isInstanceOf(AccessDeniedException.class);
    }

    private Message<byte[]> connectMessage(String token) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        accessor.setNativeHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> connectMessageWithoutAuthorization() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> subscribeMessage(AuthUserPrincipal principal, String destination) {
        return messageWithUser(StompCommand.SUBSCRIBE, principal, destination);
    }

    private Message<byte[]> sendMessage(AuthUserPrincipal principal, String destination) {
        return messageWithUser(StompCommand.SEND, principal, destination);
    }

    private Message<byte[]> sendMessageWithoutUser(String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setLeaveMutable(true);
        accessor.setDestination(destination);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> messageWithUser(StompCommand command, AuthUserPrincipal principal, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setLeaveMutable(true);
        accessor.setDestination(destination);
        accessor.setUser(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
