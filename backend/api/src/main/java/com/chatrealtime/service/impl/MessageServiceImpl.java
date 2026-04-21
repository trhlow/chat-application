package com.chatrealtime.service.impl;

import com.chatrealtime.domain.Message;
import com.chatrealtime.domain.MessageAttachment;
import com.chatrealtime.domain.Room;
import com.chatrealtime.dto.request.CreateMessageRequest;
import com.chatrealtime.dto.response.MessagePageResponse;
import com.chatrealtime.dto.response.MessageResponse;
import com.chatrealtime.dto.response.RoomUnreadCountResponse;
import com.chatrealtime.exception.BadRequestException;
import com.chatrealtime.exception.MessageNotFoundException;
import com.chatrealtime.exception.RoomNotFoundException;
import com.chatrealtime.mapper.MessageMapper;
import com.chatrealtime.repository.MessageAttachmentRepository;
import com.chatrealtime.repository.MessageRepository;
import com.chatrealtime.repository.RoomRepository;
import com.chatrealtime.security.AuthContextService;
import com.chatrealtime.security.AuthUserPrincipal;
import com.chatrealtime.service.MessageService;
import com.chatrealtime.storage.MessageAttachmentStorageService;
import com.chatrealtime.storage.StoredMessageAttachment;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;
    private static final int MAX_CONTENT_LENGTH = 4000;
    private static final int MAX_PREVIEW_LENGTH = 120;
    private static final Set<String> ALLOWED_STATUS = Set.of("sent", "delivered", "seen");

    private final MessageRepository messageRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final RoomRepository roomRepository;
    private final MessageMapper messageMapper;
    private final MessageAttachmentStorageService messageAttachmentStorageService;
    private final AuthContextService authContextService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MongoTemplate mongoTemplate;

    @Override
    public MessagePageResponse getMessagesByRoomId(String roomId, Integer limit, LocalDateTime before) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        int safeLimit = sanitizeLimit(limit);
        Room room = ensureRoomExists(roomId);
        ensureMembership(room, principal.getId());

        Page<Message> messagePage = before == null
                ? messageRepository.findByRoomIdOrderByTimestampDesc(roomId, PageRequest.of(0, safeLimit))
                : messageRepository.findByRoomIdAndTimestampBeforeOrderByTimestampDesc(
                roomId,
                before,
                PageRequest.of(0, safeLimit)
        );

        List<Message> messages = new ArrayList<>(messagePage.getContent());
        markMessagesAsDelivered(messages, principal.getId(), room);
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

    @Override
    public MessageResponse createMessage(CreateMessageRequest request) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Room room = ensureRoomExists(request.roomId());
        ensureMembership(room, principal.getId());

        Message message = Message.builder()
                .roomId(request.roomId())
                .senderId(principal.getId())
                .content(request.content().trim())
                .timestamp(LocalDateTime.now())
                .status("sent")
                .deliveredToUserIds(new HashSet<>(Set.of(principal.getId())))
                .readByUserIds(new HashSet<>(Set.of(principal.getId())))
                .build();

        Message savedMessage = messageRepository.save(message);
        updateRoomLastMessage(room, message.getContent(), null);
        MessageResponse response = messageMapper.toResponse(savedMessage, List.of());
        messagingTemplate.convertAndSend("/topic/rooms/" + request.roomId() + "/messages", response);
        return response;
    }

    @Override
    public MessageResponse createMessageWithAttachment(String roomId, String content, MultipartFile file) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Room room = ensureRoomExists(roomId);
        ensureMembership(room, principal.getId());

        String normalizedContent = normalizeOptionalContent(content);
        StoredMessageAttachment storedAttachment = messageAttachmentStorageService.store(principal.getId(), file);

        Message message = Message.builder()
                .roomId(roomId)
                .senderId(principal.getId())
                .content(normalizedContent == null ? "" : normalizedContent)
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

        updateRoomLastMessage(room, normalizedContent, attachment.getFileType());
        MessageResponse response = messageMapper.toResponse(savedMessage, List.of(attachment));
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/messages", response);
        return response;
    }

    @Override
    public MessageResponse createRealtimeMessage(String roomId, String content) {
        return createMessage(new CreateMessageRequest(roomId, content));
    }

    @Override
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

    @Override
    public void markRoomAsRead(String roomId) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Room room = ensureRoomExists(roomId);
        ensureMembership(room, principal.getId());

        List<Message> unreadMessages = findUnreadMessages(roomId, principal.getId());
        if (unreadMessages.isEmpty()) {
            return;
        }

        unreadMessages.forEach(message -> {
            updateReceiptState(message, principal.getId(), "seen");
            applyStatusFromReceipts(message, room);
        });
        List<Message> savedMessages = messageRepository.saveAll(unreadMessages);
        publishStatusUpdates(savedMessages);
    }

    @Override
    public List<RoomUnreadCountResponse> getUnreadCounts() {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        List<Room> rooms = roomRepository.findByMemberIdsContaining(principal.getId());
        Map<String, Long> unreadCountMap = getUnreadCountMap(
                rooms.stream().map(Room::getId).toList()
        );
        return rooms.stream()
                .map(room -> new RoomUnreadCountResponse(room.getId(), unreadCountMap.getOrDefault(room.getId(), 0L)))
                .toList();
    }

    @Override
    public Map<String, Long> getUnreadCountMap(Collection<String> roomIds) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        if (roomIds == null || roomIds.isEmpty()) {
            return Map.of();
        }

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("roomId").in(roomIds)
                        .and("senderId").ne(principal.getId())
                        .and("readByUserIds").ne(principal.getId())),
                Aggregation.group("roomId").count().as("unreadCount")
        );

        return mongoTemplate.aggregate(aggregation, "messages", Document.class)
                .getMappedResults()
                .stream()
                .collect(Collectors.toMap(
                        document -> document.getString("_id"),
                        document -> document.get("unreadCount", Number.class).longValue()
                ));
    }

    private Map<String, List<MessageAttachment>> getAttachmentsByMessageId(List<Message> messages) {
        List<String> messageIds = messages.stream().map(Message::getId).toList();
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
        if ("read".equals(normalized)) {
            normalized = "seen";
        }
        if (!ALLOWED_STATUS.contains(normalized)) {
            throw new BadRequestException("status must be one of sent, delivered, seen");
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
        if ("seen".equals(status)) {
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
            message.setStatus("seen");
            return;
        }

        Set<String> delivered = message.getDeliveredToUserIds() == null ? Set.of() : message.getDeliveredToUserIds();
        Set<String> readBy = message.getReadByUserIds() == null ? Set.of() : message.getReadByUserIds();
        if (readBy.containsAll(recipients)) {
            message.setStatus("seen");
            return;
        }
        if (delivered.containsAll(recipients)) {
            message.setStatus("delivered");
            return;
        }
        message.setStatus("sent");
    }

    private void updateRoomLastMessage(Room room, String content, String attachmentType) {
        room.setLastMessageAt(Instant.now());
        room.setLastMessagePreview(buildMessagePreview(content, attachmentType));
        room.setUpdatedAt(Instant.now());
        roomRepository.save(room);
    }

    private String buildMessagePreview(String content, String attachmentType) {
        if (content != null && !content.isBlank()) {
            return content.length() <= MAX_PREVIEW_LENGTH
                    ? content
                    : content.substring(0, MAX_PREVIEW_LENGTH - 3) + "...";
        }
        if (attachmentType == null || attachmentType.isBlank()) {
            return "";
        }
        return switch (attachmentType.toLowerCase(Locale.ROOT)) {
            case "image" -> "[Image]";
            case "video" -> "[Video]";
            default -> "[File]";
        };
    }

    private void markMessagesAsDelivered(List<Message> messages, String actorUserId, Room room) {
        List<Message> deliverableMessages = messages.stream()
                .filter(message -> !actorUserId.equals(message.getSenderId()))
                .filter(message -> message.getDeliveredToUserIds() == null || !message.getDeliveredToUserIds().contains(actorUserId))
                .toList();
        if (deliverableMessages.isEmpty()) {
            return;
        }

        deliverableMessages.forEach(message -> {
            updateReceiptState(message, actorUserId, "delivered");
            applyStatusFromReceipts(message, room);
        });
        List<Message> savedMessages = messageRepository.saveAll(deliverableMessages);
        publishStatusUpdates(savedMessages);
    }

    private void publishStatusUpdates(List<Message> messages) {
        Map<String, List<MessageAttachment>> attachmentsByMessageId = getAttachmentsByMessageId(messages);
        messages.forEach(message -> messagingTemplate.convertAndSend(
                "/topic/rooms/" + message.getRoomId() + "/status",
                messageMapper.toResponse(message, attachmentsByMessageId.getOrDefault(message.getId(), List.of()))
        ));
    }

    private List<Message> findUnreadMessages(String roomId, String actorUserId) {
        Criteria criteria = Criteria.where("roomId").is(roomId)
                .and("senderId").ne(actorUserId)
                .and("readByUserIds").ne(actorUserId);

        return mongoTemplate.find(Query.query(criteria), Message.class);
    }

}
