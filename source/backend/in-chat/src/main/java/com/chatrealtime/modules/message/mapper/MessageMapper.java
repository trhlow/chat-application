package com.chatrealtime.modules.message.mapper;

import com.chatrealtime.modules.message.dto.response.MessageResponse;
import com.chatrealtime.modules.message.model.Message;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class MessageMapper {
    public MessageResponse toResponse(Message message) {
        Set<String> delivered = message.getDeliveredToUserIds() == null ? Set.of() : message.getDeliveredToUserIds();
        Set<String> readBy = message.getReadByUserIds() == null ? Set.of() : message.getReadByUserIds();
        return new MessageResponse(
                message.getId(),
                message.getRoomId(),
                message.getSenderId(),
                message.getContent(),
                message.getTimestamp(),
                message.getStatus(),
                delivered,
                readBy
        );
    }
}


