package com.chatrealtime.modules.message.service;

import com.chatrealtime.modules.message.dto.CreateMessageRequest;
import com.chatrealtime.modules.message.dto.response.MessagePageResponse;
import com.chatrealtime.modules.message.dto.response.MessageResponse;
import com.chatrealtime.exception.BadRequestException;
import com.chatrealtime.exception.MessageNotFoundException;
import com.chatrealtime.exception.RoomNotFoundException;
import com.chatrealtime.modules.message.mapper.MessageMapper;
import com.chatrealtime.modules.message.model.Message;
import com.chatrealtime.modules.message.model.MessageAttachment;
import com.chatrealtime.modules.room.model.Room;
import com.chatrealtime.modules.message.repository.MessageAttachmentRepository;
import com.chatrealtime.modules.message.repository.MessageRepository;
import com.chatrealtime.modules.message.storage.MessageAttachmentStorageService;
import com.chatrealtime.modules.message.storage.StoredMessageAttachment;
import com.chatrealtime.modules.room.repository.RoomRepository;
import com.chatrealtime.security.AuthContextService;
import com.chatrealtime.security.AuthUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collections;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;
    private static final int MAX_CONTENT_LENGTH = 4000;
    private static final Set<String> ALLOWED_STATUS = Set.of("sent", "delivered", "read");

    private final MessageRepository messageRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final RoomRepository roomRepository;
    private final MessageMapper messageMapper;
    private final MessageAttachmentStorageService messageAttachmentStorageService;
    private final AuthContextService authContextService;
    private final SimpMessagingTemplate messagingTemplate;

    public MessagePageResponse getMessagesByRoomId(String roomId, Integer limit, LocalDateTime before) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        int safeLimit = sanitizeLimit(limit);
        Room room = ensureRoomExists(roomId);
        ensureMembership(room, principal.getId());

        Page<Message> messagePage;
        if (before != null) {
            messagePage = messageRepository.findByRoomIdAndTimestampBeforeOrderByTimestampDesc(
                    roomId,
                    before,
                    PageRequest.of(0, safeLimit)
            );
        } else {
            messagePage = messageRepository.findByRoomIdOrderByTimestampDesc(roomId, PageRequest.of(0, safeLimit));
        }

        List<Message> messages = new ArrayList<>(messagePage.getContent());
        Collections.reverse(messages);
        Map<String, List<MessageAttachment>> attachmentsByMessageId = getAttachmentsByMessageId(messages);
        List<MessageResponse> items = messages.stream()
                .map(message -> messageMapper.toResponse(
                        message,
                        attachmentsByMessageId.getOrDefault(message.getId(), List.of())
                ))
                .toList();
        LocalDateTime nextBefore = items.isEmpty() ? null : items.get(0).timestamp().truncatedTo(ChronoUnit.MILLIS);
        return new MessagePageResponse(items, nextBefore, messagePage.hasNext());
    }

    public MessageResponse createMessage(CreateMessageRequest request) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Room room = ensureRoomExists(request.getRoomId());
        ensureMembership(room, principal.getId());

        Message message = Message.builder()
                .roomId(request.getRoomId())
                .senderId(principal.getId())
                .content(request.getContent().trim())
                .timestamp(LocalDateTime.now())
                .status("sent")
                .deliveredToUserIds(new HashSet<>(Set.of(principal.getId())))
                .readByUserIds(new HashSet<>(Set.of(principal.getId())))
                .build();

        Message savedMessage = messageRepository.save(message);
        MessageResponse response = messageMapper.toResponse(savedMessage, List.of());
        messagingTemplate.convertAndSend("/topic/rooms/" + request.getRoomId() + "/messages", response);
        return response;
    }

    public MessageResponse createMessageWithAttachment(String roomId, String content, MultipartFile file) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Room room = ensureRoomExists(roomId);
        ensureMembership(room, principal.getId());

        String normalizedContent = normalizeOptionalContent(content);
        StoredMessageAttachment storedAttachment = messageAttachmentStorageService.store(principal.getId(), file);

        Message message = Message.builder()
                .roomId(roomId)
                .senderId(principal.getId())
                .content(normalizedContent)
                .timestamp(LocalDateTime.now())
                .status("sent")
                .deliveredToUserIds(new HashSet<>(Set.of(principal.getId())))
                .readByUserIds(new HashSet<>(Set.of(principal.getId())))
                .build();

        Message savedMessage = messageRepository.save(message);
        MessageAttachment attachment = messageAttachmentRepository.save(MessageAttachment.builder()
                .messageId(savedMessage.getId())
                .fileUrl(storedAttachment.fileUrl())
                .fileType(storedAttachment.fileType())
                .mimeType(storedAttachment.mimeType())
                .fileSize(storedAttachment.fileSize())
                .originalName(storedAttachment.originalName())
                .thumbnailUrl(storedAttachment.thumbnailUrl())
                .storageProvider(storedAttachment.storageProvider())
                .storagePublicId(storedAttachment.storagePublicId())
                .createdAt(Instant.now())
                .build());

        MessageResponse response = messageMapper.toResponse(savedMessage, List.of(attachment));
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/messages", response);
        return response;
    }

    public MessageResponse createRealtimeMessage(String roomId, String content) {
        CreateMessageRequest request = new CreateMessageRequest();
        request.setRoomId(roomId);
        request.setContent(content);
        return createMessage(request);
    }

    public MessageResponse updateMessageStatus(String messageId, String status) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        String normalizedStatus = normalizeStatus(status);
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException("Message not found"));

        Room room = ensureRoomExists(message.getRoomId());
        ensureMembership(room, principal.getId());
        if ("sent".equals(normalizedStatus)) {
            throw new BadRequestException("status cannot be reverted to sent");
        }

        updateReceiptState(message, principal.getId(), normalizedStatus);
        applyStatusFromReceipts(message, room);

        Message saved = messageRepository.save(message);
        MessageResponse response = messageMapper.toResponse(saved, messageAttachmentRepository.findByMessageId(saved.getId()));
        messagingTemplate.convertAndSend("/topic/rooms/" + message.getRoomId() + "/status", response);
        return response;
    }

    private Map<String, List<MessageAttachment>> getAttachmentsByMessageId(List<Message> messages) {
        List<String> messageIds = messages.stream()
                .map(Message::getId)
                .toList();
        if (messageIds.isEmpty()) {
            return Map.of();
        }
        return messageAttachmentRepository.findByMessageIdIn(messageIds)
                .stream()
                .collect(Collectors.groupingBy(MessageAttachment::getMessageId));
    }

    private String normalizeOptionalContent(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        String normalized = content.trim();
        if (normalized.length() > MAX_CONTENT_LENGTH) {
            throw new BadRequestException("content must be at most 4000 characters");
        }
        return normalized;
    }

    private Room ensureRoomExists(String roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found"));
    }

    private void ensureMembership(Room room, String userId) {
        if (room.getMemberIds() == null || !room.getMemberIds().contains(userId)) {
            throw new BadRequestException("Current user is not a member of this room");
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

    private void updateReceiptState(Message message, String actorUserId, String status) {
        Set<String> delivered = message.getDeliveredToUserIds() == null
                ? new HashSet<>()
                : new HashSet<>(message.getDeliveredToUserIds());
        Set<String> readBy = message.getReadByUserIds() == null
                ? new HashSet<>()
                : new HashSet<>(message.getReadByUserIds());

        if ("delivered".equals(status)) {
            delivered.add(actorUserId);
        }
        if ("read".equals(status)) {
            delivered.add(actorUserId);
            readBy.add(actorUserId);
        }

        message.setDeliveredToUserIds(delivered);
        message.setReadByUserIds(readBy);
    }

    private void applyStatusFromReceipts(Message message, Room room) {
        List<String> recipients = room.getMemberIds().stream()
                .filter(memberId -> !memberId.equals(message.getSenderId()))
                .toList();

        if (recipients.isEmpty()) {
            message.setStatus("read");
            return;
        }

        Set<String> delivered = message.getDeliveredToUserIds() == null ? Set.of() : message.getDeliveredToUserIds();
        Set<String> readBy = message.getReadByUserIds() == null ? Set.of() : message.getReadByUserIds();
        if (readBy.containsAll(recipients)) {
            message.setStatus("read");
            return;
        }
        if (delivered.containsAll(recipients)) {
            message.setStatus("delivered");
            return;
        }
        message.setStatus("sent");
    }
}



