package com.chatrealtime.unit;

import com.chatrealtime.controller.RealtimeController;
import com.chatrealtime.dto.request.RealtimeMessageRequest;
import com.chatrealtime.dto.request.UpdateMessageStatusRequest;
import com.chatrealtime.dto.response.MessageResponse;
import com.chatrealtime.exception.InvalidCredentialsException;
import com.chatrealtime.security.AuthUserPrincipal;
import com.chatrealtime.service.MessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RealtimeControllerTest {
    @Mock
    private MessageService messageService;

    @Test
    void sendMessage_ShouldUseAuthenticatedPrincipalFromWebSocketSession() {
        RealtimeController controller = new RealtimeController(messageService);
        AuthUserPrincipal principal = new AuthUserPrincipal("u1", "alice", "pw", 2);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        MessageResponse expected = messageResponse();
        when(messageService.createRealtimeMessage(eq(principal), eq("room-1"), eq("hello")))
                .thenReturn(expected);

        MessageResponse response = controller.sendMessage("room-1", authentication, new RealtimeMessageRequest("hello"));

        assertThat(response).isEqualTo(expected);
        verify(messageService).createRealtimeMessage(principal, "room-1", "hello");
    }

    @Test
    void updateMessageStatus_ShouldRejectUnauthenticatedPrincipal() {
        RealtimeController controller = new RealtimeController(messageService);

        assertThatThrownBy(() -> controller.updateMessageStatus(
                "message-1",
                () -> "anonymous",
                new UpdateMessageStatusRequest("seen")
        )).isInstanceOf(InvalidCredentialsException.class);
    }

    private MessageResponse messageResponse() {
        return new MessageResponse(
                "m1",
                "room-1",
                "u1",
                "hello",
                LocalDateTime.now(),
                "sent",
                Set.of("u1"),
                Set.of("u1"),
                List.of()
        );
    }
}
