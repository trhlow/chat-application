package com.chatrealtime.modules.realtime.controller;

import com.chatrealtime.modules.message.dto.RealtimeMessageRequest;
import com.chatrealtime.modules.message.dto.UpdateMessageStatusRequest;
import com.chatrealtime.modules.message.dto.response.MessageResponse;
import com.chatrealtime.modules.message.service.MessageService;
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


