package com.chatrealtime.controller;

import com.chatrealtime.dto.request.RealtimeMessageRequest;
import com.chatrealtime.dto.request.UpdateMessageStatusRequest;
import com.chatrealtime.dto.response.MessageResponse;
import com.chatrealtime.security.AuthUserPrincipal;
import com.chatrealtime.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class RealtimeController {
    private final MessageService messageService;

    @MessageMapping("/rooms/{roomId}/messages")
    public MessageResponse sendMessage(
            @DestinationVariable String roomId,
            Principal principal,
            @Valid RealtimeMessageRequest request
    ) {
        return messageService.createRealtimeMessage(requireAuthPrincipal(principal), roomId, request.content());
    }

    @MessageMapping("/messages/{messageId}/status")
    public MessageResponse updateMessageStatus(
            @DestinationVariable String messageId,
            Principal principal,
            @Valid UpdateMessageStatusRequest request
    ) {
        return messageService.updateRealtimeMessageStatus(requireAuthPrincipal(principal), messageId, request.status());
    }

    private AuthUserPrincipal requireAuthPrincipal(Principal principal) {
        if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken authentication
                && authentication.getPrincipal() instanceof AuthUserPrincipal authUserPrincipal) {
            return authUserPrincipal;
        }
        throw new com.chatrealtime.exception.InvalidCredentialsException("Unauthorized");
    }
}


