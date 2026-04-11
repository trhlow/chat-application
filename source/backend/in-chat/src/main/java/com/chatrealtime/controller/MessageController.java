package com.chatrealtime.controller;

import com.chatrealtime.dto.message.CreateMessageRequest;
import com.chatrealtime.dto.message.UpdateMessageStatusRequest;
import jakarta.validation.Valid;
import com.chatrealtime.model.Message;
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
import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {
    private final MessageService messageService;

    @GetMapping
    public List<Message> getMessages(
            @RequestParam String roomId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) LocalDateTime before
    ) {
        return messageService.getMessagesByRoomId(roomId, limit, before);
    }

    @PostMapping
    public Message createMessage(@Valid @RequestBody CreateMessageRequest request) {
        return messageService.createMessage(request);
    }

    @PatchMapping("/{messageId}/status")
    public Message updateMessageStatus(
            @PathVariable String messageId,
            @Valid @RequestBody UpdateMessageStatusRequest request
    ) {
        return messageService.updateMessageStatus(messageId, request.getStatus());
    }
}

