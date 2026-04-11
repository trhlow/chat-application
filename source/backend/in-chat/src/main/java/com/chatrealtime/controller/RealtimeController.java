package com.chatrealtime.controller;

import com.chatrealtime.dto.message.RealtimeMessageRequest;
import com.chatrealtime.dto.message.UpdateMessageStatusRequest;
import com.chatrealtime.dto.message.response.MessageResponse;
import com.chatrealtime.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class RealtimeController {
    private final MessageService messageService;

    @MessageMapping("/rooms/{roomId}/messages")
    public MessageResponse sendMessage(
            @DestinationVariable String roomId,
            @Valid RealtimeMessageRequest request
    ) {
        return messageService.createRealtimeMessage(roomId, request.content());
    }

    @MessageMapping("/messages/{messageId}/status")
    public MessageResponse updateMessageStatus(
            @DestinationVariable String messageId,
            @Valid UpdateMessageStatusRequest request
    ) {
        return messageService.updateMessageStatus(messageId, request.getStatus());
    }
}
