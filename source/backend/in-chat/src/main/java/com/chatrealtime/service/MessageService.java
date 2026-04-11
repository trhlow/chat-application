package com.chatrealtime.service;

import com.chatrealtime.dto.message.CreateMessageRequest;
import com.chatrealtime.exception.BadRequestException;
import com.chatrealtime.exception.InvalidCredentialsException;
import com.chatrealtime.exception.MessageNotFoundException;
import com.chatrealtime.exception.RoomNotFoundException;
import com.chatrealtime.model.Message;
import com.chatrealtime.model.Room;
import com.chatrealtime.repository.MessageRepository;
import com.chatrealtime.repository.RoomRepository;
import com.chatrealtime.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MessageService {
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;
    private static final Set<String> ALLOWED_STATUS = Set.of("sent", "delivered", "read");

    private final MessageRepository messageRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public List<Message> getMessagesByRoomId(String roomId, Integer limit, LocalDateTime before) {
        int safeLimit = sanitizeLimit(limit);
        ensureRoomExists(roomId);

        List<Message> messages;
        if (before != null) {
            List<Message> descMessages = messageRepository.findByRoomIdAndTimestampBeforeOrderByTimestampDesc(roomId, before);
            int toIndex = Math.min(safeLimit, descMessages.size());
            messages = new ArrayList<>(descMessages.subList(0, toIndex));
            Collections.reverse(messages);
            return messages;
        }

        List<Message> allMessages = messageRepository.findByRoomIdOrderByTimestampAsc(roomId);
        if (allMessages.size() <= safeLimit) {
            return allMessages;
        }
        return allMessages.subList(allMessages.size() - safeLimit, allMessages.size());
    }

    public Message createMessage(CreateMessageRequest request) {
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new RoomNotFoundException("Room not found"));

        if (!userRepository.existsById(request.getSenderId())) {
            throw new InvalidCredentialsException("Sender does not exist");
        }

        if (room.getMemberIds() == null || !room.getMemberIds().contains(request.getSenderId())) {
            throw new BadRequestException("Sender is not a member of this room");
        }

        Message message = Message.builder()
                .roomId(request.getRoomId())
                .senderId(request.getSenderId())
                .content(request.getContent().trim())
                .timestamp(LocalDateTime.now())
                .status("sent")
                .build();

        return messageRepository.save(message);
    }

    public Message updateMessageStatus(String messageId, String status) {
        String normalizedStatus = normalizeStatus(status);
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException("Message not found"));

        message.setStatus(normalizedStatus);
        return messageRepository.save(message);
    }

    private void ensureRoomExists(String roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new RoomNotFoundException("Room not found");
        }
    }

    private int sanitizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit <= 0) {
            throw new BadRequestException("limit must be greater than 0");
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new BadRequestException("status is required");
        }

        String normalized = status.toLowerCase(Locale.ROOT).trim();
        if (!ALLOWED_STATUS.contains(normalized)) {
            throw new BadRequestException("status must be one of sent, delivered, read");
        }
        return normalized;
    }
}

