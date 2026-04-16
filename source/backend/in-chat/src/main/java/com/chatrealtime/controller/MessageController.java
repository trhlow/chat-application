package com.chatrealtime.controller;

import com.chatrealtime.dto.request.CreateMessageRequest;
import com.chatrealtime.dto.request.UpdateMessageStatusRequest;
import com.chatrealtime.dto.response.MessagePageResponse;
import com.chatrealtime.dto.response.MessageResponse;
import jakarta.validation.Valid;
import com.chatrealtime.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {
    private final MessageService messageService;

    @GetMapping
    public MessagePageResponse getMessages(
            @RequestParam String roomId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) LocalDateTime before
    ) {
        return messageService.getMessagesByRoomId(roomId, limit, before);
    }

    @PostMapping
    public MessageResponse createMessage(@Valid @RequestBody CreateMessageRequest request) {
        return messageService.createMessage(request);
    }

    @PatchMapping("/{messageId}/status")
    public MessageResponse updateMessageStatus(
            @PathVariable String messageId,
            @Valid @RequestBody UpdateMessageStatusRequest request
    ) {
        return messageService.updateMessageStatus(messageId, request.status());
    }
}



