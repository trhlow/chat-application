package com.chatrealtime.service.impl;

import com.chatrealtime.config.AppMessagesProperties;
import com.chatrealtime.domain.GroupSettings;
import com.chatrealtime.domain.Message;
import com.chatrealtime.domain.MessageAttachment;
import com.chatrealtime.domain.Room;
import com.chatrealtime.domain.User;
import com.chatrealtime.dto.request.CreateMessageRequest;
import com.chatrealtime.dto.response.MessagePageResponse;
import com.chatrealtime.dto.response.MessageResponse;
import com.chatrealtime.dto.response.RoomUnreadCountResponse;
import com.chatrealtime.dto.response.TypingIndicatorResponse;
import com.chatrealtime.exception.BadRequestException;
import com.chatrealtime.exception.MessageNotFoundException;
import com.chatrealtime.exception.RoomNotFoundException;
import com.chatrealtime.mapper.MessageMapper;
import com.chatrealtime.repository.MessageAttachmentRepository;
import com.chatrealtime.repository.MessageRepository;
import com.chatrealtime.repository.RoomRepository;
import com.chatrealtime.repository.UserBlockRepository;
import com.chatrealtime.repository.UserRepository;
import com.chatrealtime.security.AuthContextService;
import com.chatrealtime.security.AuthUserPrincipal;
import com.chatrealtime.service.MessageService;
import com.chatrealtime.service.NotificationService;
import com.chatrealtime.service.PresenceService;
import com.chatrealtime.storage.MessageAttachmentStorageService;
import com.chatrealtime.storage.StoredMessageAttachment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
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
import java.util.regex.Pattern;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MessageServiceImpl implements MessageService {
    private static final String NOTIFICATION_NEW_MESSAGE = "new_message";
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;
    /** Bound memory and write bursts when marking a room as read. */
    private static final int MARK_READ_BATCH_SIZE = 500;
    private static final int MAX_CONTENT_LENGTH = 4000;
    private static final int MAX_PREVIEW_LENGTH = 120;
    private static final Set<String> ALLOWED_STATUS = Set.of("sent", "delivered", "seen");
    private static final Set<String> ALLOWED_MESSAGE_TYPES = Set.of("TEXT", "IMAGE", "FILE", "SYSTEM");
    private static final String TYPE_TEXT = "TEXT";
    private static final String TYPE_IMAGE = "IMAGE";
    private static final String TYPE_FILE = "FILE";

    private final MessageRepository messageRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final RoomRepository roomRepository;
    private final MessageMapper messageMapper;
    private final MessageAttachmentStorageService messageAttachmentStorageService;
    private final AuthContextService authContextService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MongoTemplate mongoTemplate;
    private final UserRepository userRepository;
    private final UserBlockRepository userBlockRepository;
    private final NotificationService notificationService;
    private final PresenceService presenceService;
    private final AppMessagesProperties appMessagesProperties;

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

        List<Message> messages = messagePage.getContent()
                .stream()
                .filter(message -> !isDeletedForUser(message, principal.getId()))
                .toList();
        markMessagesAsDelivered(messages, principal.getId(), room);
        messages = new ArrayList<>(messages);
        Collections.reverse(messages);
        List<MessageResponse> items = mapMessagesForUser(messages, principal.getId());
        LocalDateTime nextBefore = items.isEmpty() ? null : items.get(0).timestamp().truncatedTo(ChronoUnit.MILLIS);
        return new MessagePageResponse(items, nextBefore, messagePage.hasNext());
    }

    @Override
    public MessageResponse createMessage(CreateMessageRequest request) {
        return createMessage(authContextService.requireCurrentUser(), request);
    }

    @Override
    public MessageResponse createMessageWithAttachment(String roomId, String content, MultipartFile file) {
        return createMessageWithAttachment(roomId, content, null, file);
    }

    @Override
    public MessageResponse createMessageWithAttachment(String roomId, String content, String clientMessageId, MultipartFile file) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Room room = ensureRoomExists(roomId);
        ensureMembership(room, principal.getId());
        ensureNotBlockedInDirectRoom(room, principal.getId());
        ensureCanSendInGroup(room, principal.getId());

        String normalizedContent = normalizeOptionalContent(content);
        String normalizedClientMessageId = normalizeClientMessageId(clientMessageId);
        MessageResponse existing = findExistingClientMessage(roomId, principal.getId(), normalizedClientMessageId);
        if (existing != null) {
            return existing;
        }
        StoredMessageAttachment storedAttachment = messageAttachmentStorageService.store(principal.getId(), file);

        try {
            Message message = Message.builder()
                    .roomId(roomId)
                    .senderId(principal.getId())
                    .content(normalizedContent == null ? "" : normalizedContent)
                    .type(resolveAttachmentMessageType(storedAttachment.fileType()))
                    .clientMessageId(normalizedClientMessageId)
                    .timestamp(LocalDateTime.now())
                    .status("sent")
                    .deliveredToUserIds(new HashSet<>(Set.of(principal.getId())))
                    .readByUserIds(new HashSet<>(Set.of(principal.getId())))
                    .updatedAt(LocalDateTime.now())
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
            notifyOfflineRecipients(
                    room,
                    principal.getId(),
                    buildMessagePreview(normalizedContent, attachment.getFileType()),
                    savedMessage.getId()
            );
            MessageResponse response = messageMapper.toResponse(savedMessage, List.of(attachment));
            messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/messages", response);
            return response;
        } catch (RuntimeException exception) {
            cleanupStoredAttachment(storedAttachment);
            throw exception;
        }
    }

    @Override
    public MessageResponse createRealtimeMessage(AuthUserPrincipal principal, String roomId, String content) {
        return createMessage(principal, new CreateMessageRequest(roomId, content));
    }

    @Override
    public MessageResponse createRealtimeMessage(
            AuthUserPrincipal principal,
            String roomId,
            String content,
            String type,
            String replyToMessageId
    ) {
        return createRealtimeMessage(principal, roomId, content, type, replyToMessageId, null);
    }

    @Override
    public MessageResponse createRealtimeMessage(
            AuthUserPrincipal principal,
            String roomId,
            String content,
            String type,
            String replyToMessageId,
            String clientMessageId
    ) {
        return createMessage(principal, new CreateMessageRequest(roomId, content, type, replyToMessageId, clientMessageId));
    }

    @Override
    public MessageResponse updateMessageStatus(String messageId, String status) {
        return updateMessageStatus(authContextService.requireCurrentUser(), messageId, status);
    }

    @Override
    public MessageResponse updateRealtimeMessageStatus(AuthUserPrincipal principal, String messageId, String status) {
        return updateMessageStatus(principal, messageId, status);
    }

    @Override
    public MessageResponse recallMessage(String messageId) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException("Message not found"));
        Room room = ensureRoomExists(message.getRoomId());
        ensureMembership(room, principal.getId());
        if (!principal.getId().equals(message.getSenderId())) {
            throw new AccessDeniedException("Forbidden");
        }
        ensureRecallWindowAllows(message);
        if (!message.isRecalled()) {
            LocalDateTime now = LocalDateTime.now();
            message.setRecalled(true);
            message.setRecalledAt(now);
            message.setUpdatedAt(now);
            message = messageRepository.save(message);
        }

        MessageResponse response = toResponseForUser(message, principal.getId());
        messagingTemplate.convertAndSend("/topic/rooms/" + message.getRoomId() + "/messages", response);
        return response;
    }

    @Override
    public void deleteMessageForCurrentUser(String messageId) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException("Message not found"));
        Room room = ensureRoomExists(message.getRoomId());
        ensureMembership(room, principal.getId());

        Set<String> deletedForUserIds = message.getDeletedForUserIds() == null
                ? new HashSet<>()
                : new HashSet<>(message.getDeletedForUserIds());
        if (deletedForUserIds.add(principal.getId())) {
            message.setDeletedForUserIds(deletedForUserIds);
            message.setUpdatedAt(LocalDateTime.now());
            messageRepository.save(message);
        }
    }

    @Override
    public MessagePageResponse searchMessages(String roomId, String keyword, int page, int size) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Room room = ensureRoomExists(roomId);
        ensureMembership(room, principal.getId());

        String normalizedKeyword = normalizeSearchKeyword(keyword);
        int safePage = Math.max(page, 0);
        int safeSize = sanitizeLimit(size);
        Criteria textTypeCriteria = new Criteria().orOperator(
                Criteria.where("type").exists(false),
                Criteria.where("type").is(null),
                Criteria.where("type").is(TYPE_TEXT),
                Criteria.where("type").is(TYPE_TEXT.toLowerCase(Locale.ROOT))
        );
        Criteria criteria = new Criteria().andOperator(
                Criteria.where("roomId").is(roomId),
                Criteria.where("content").regex(Pattern.compile(Pattern.quote(normalizedKeyword), Pattern.CASE_INSENSITIVE)),
                Criteria.where("recalled").ne(true),
                Criteria.where("deletedForUserIds").ne(principal.getId()),
                textTypeCriteria
        );
        Query query = Query.query(criteria)
                .with(Sort.by(Sort.Direction.DESC, "timestamp"))
                .skip((long) safePage * safeSize)
                .limit(safeSize + 1);

        List<Message> matches = mongoTemplate.find(query, Message.class);
        boolean hasMore = matches.size() > safeSize;
        if (hasMore) {
            matches = matches.subList(0, safeSize);
        }
        return new MessagePageResponse(mapMessagesForUser(matches, principal.getId()), null, hasMore);
    }

    @Override
    public void publishTypingIndicator(AuthUserPrincipal principal, String roomId, boolean typing) {
        Room room = ensureRoomExists(roomId);
        ensureMembership(room, principal.getId());
        messagingTemplate.convertAndSend(
                "/topic/rooms/" + roomId + "/typing",
                new TypingIndicatorResponse(roomId, principal.getId(), principal.getUsername(), typing, Instant.now())
        );
    }

    @Override
    public void markRoomAsRead(String roomId) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Room room = ensureRoomExists(roomId);
        ensureMembership(room, principal.getId());

        for (int batch = 0; batch < appMessagesProperties.markReadMaxBatches(); batch++) {
            List<Message> unreadMessages = findUnreadMessages(roomId, principal.getId(), MARK_READ_BATCH_SIZE);
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
    public RoomUnreadCountResponse getUnreadCount(String roomId) {
        long count = getUnreadCountMap(List.of(roomId)).getOrDefault(roomId, 0L);
        return new RoomUnreadCountResponse(roomId, count);
    }

    @Override
    public Map<String, Long> getUnreadCountMap(Collection<String> roomIds) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        if (roomIds == null || roomIds.isEmpty()) {
            return Map.of();
        }

        ensurePrincipalMemberOfAllRooms(principal, roomIds);

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("roomId").in(roomIds)
                        .and("senderId").ne(principal.getId())
                        .and("readByUserIds").ne(principal.getId())
                        .and("deletedForUserIds").ne(principal.getId())),
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

    private List<MessageResponse> mapMessagesForUser(List<Message> messages, String userId) {
        Map<String, List<MessageAttachment>> attachmentsByMessageId = getAttachmentsByMessageId(messages);
        Map<String, Message> replyMessagesById = getReplyMessagesById(messages);
        return messages.stream()
                .map(message -> messageMapper.toResponse(
                        message,
                        attachmentsByMessageId.getOrDefault(message.getId(), List.of()),
                        visibleReplyMessage(getReplyMessage(replyMessagesById, message.getReplyToMessageId()), userId)
                ))
                .toList();
    }

    private Message getReplyMessage(Map<String, Message> replyMessagesById, String replyToMessageId) {
        if (replyToMessageId == null || replyToMessageId.isBlank()) {
            return null;
        }
        return replyMessagesById.get(replyToMessageId);
    }

    private MessageResponse toResponseForUser(Message message, String userId) {
        Message replyToMessage = null;
        if (message.getReplyToMessageId() != null && !message.getReplyToMessageId().isBlank()) {
            replyToMessage = messageRepository.findById(message.getReplyToMessageId())
                    .map(reply -> visibleReplyMessage(reply, userId))
                    .orElse(null);
        }
        List<MessageAttachment> attachments = messageAttachmentRepository.findByMessageId(message.getId());
        if (replyToMessage == null) {
            return messageMapper.toResponse(message, attachments);
        }
        return messageMapper.toResponse(message, attachments, replyToMessage);
    }

    private Map<String, Message> getReplyMessagesById(List<Message> messages) {
        List<String> replyMessageIds = messages.stream()
                .map(Message::getReplyToMessageId)
                .filter(replyToMessageId -> replyToMessageId != null && !replyToMessageId.isBlank())
                .distinct()
                .toList();
        if (replyMessageIds.isEmpty()) {
            return Map.of();
        }
        return messageRepository.findAllById(replyMessageIds)
                .stream()
                .collect(Collectors.toMap(Message::getId, message -> message, (left, right) -> left));
    }

    private Message visibleReplyMessage(Message replyToMessage, String userId) {
        if (replyToMessage == null || isDeletedForUser(replyToMessage, userId)) {
            return null;
        }
        return replyToMessage;
    }

    private String normalizeOptionalContent(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        String normalized = content.trim();
        if (normalized.length() > MAX_CONTENT_LENGTH) {
            throw new BadRequestException("content must be at most 4000 characters");
        }
        return org.springframework.web.util.HtmlUtils.htmlEscape(normalized);
    }

    private String normalizeRequiredContent(String content) {
        String normalized = normalizeOptionalContent(content);
        if (normalized == null) {
            throw new BadRequestException("content is required");
        }
        return normalized;
    }

    private String normalizeMessageType(String type) {
        if (type == null || type.isBlank()) {
            return TYPE_TEXT;
        }
        String normalized = type.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_MESSAGE_TYPES.contains(normalized)) {
            throw new BadRequestException("type must be one of TEXT, IMAGE, FILE, SYSTEM");
        }
        return normalized;
    }

    private String normalizeSearchKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new BadRequestException("keyword is required");
        }
        return keyword.trim();
    }

    private String normalizeReplyToMessageId(String replyToMessageId) {
        if (replyToMessageId == null || replyToMessageId.isBlank()) {
            return null;
        }
        return replyToMessageId.trim();
    }

    private String normalizeClientMessageId(String clientMessageId) {
        if (clientMessageId == null || clientMessageId.isBlank()) {
            return null;
        }
        String normalized = clientMessageId.trim();
        if (normalized.length() > 100) {
            throw new BadRequestException("clientMessageId must be at most 100 characters");
        }
        return normalized;
    }

    private Message resolveReplyToMessage(String replyToMessageId, String roomId) {
        String normalizedReplyToMessageId = normalizeReplyToMessageId(replyToMessageId);
        if (normalizedReplyToMessageId == null) {
            return null;
        }
        Message replyToMessage = messageRepository.findById(normalizedReplyToMessageId)
                .orElseThrow(() -> new MessageNotFoundException("Reply message not found"));
        if (!roomId.equals(replyToMessage.getRoomId())) {
            throw new BadRequestException("replyToMessageId must belong to the same room");
        }
        return replyToMessage;
    }

    private boolean isDeletedForUser(Message message, String userId) {
        return message.getDeletedForUserIds() != null && message.getDeletedForUserIds().contains(userId);
    }

    private String resolveAttachmentMessageType(String attachmentType) {
        if ("image".equalsIgnoreCase(attachmentType)) {
            return TYPE_IMAGE;
        }
        return TYPE_FILE;
    }

    private void ensureRecallWindowAllows(Message message) {
        if (!appMessagesProperties.enforceRecallTimeLimit() || message.getTimestamp() == null) {
            return;
        }
        if (message.getTimestamp().isBefore(LocalDateTime.now().minusMinutes(appMessagesProperties.recallTimeLimitMinutes()))) {
            throw new BadRequestException("Message recall time limit has expired");
        }
    }

    private Room ensureRoomExists(String roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found"));
    }

    private void ensureMembership(Room room, String userId) {
        if (room.getMemberIds() == null || !room.getMemberIds().contains(userId)) {
            throw new AccessDeniedException("Forbidden");
        }
    }

    private void ensureNotBlockedInDirectRoom(Room room, String userId) {
        if (!"direct".equalsIgnoreCase(room.getType()) || room.getMemberIds() == null) {
            return;
        }
        room.getMemberIds().stream()
                .filter(memberId -> !memberId.equals(userId))
                .findFirst()
                .ifPresent(otherUserId -> {
                    if (userBlockRepository.existsBetweenUsers(userId, otherUserId)) {
                        throw new AccessDeniedException("Blocked users cannot send direct messages");
                    }
                });
    }

    private void ensureCanSendInGroup(Room room, String userId) {
        if (!"group".equalsIgnoreCase(room.getType())) {
            return;
        }
        GroupSettings settings = room.getSettings() == null ? GroupSettings.defaults() : room.getSettings();
        if (GroupSettings.PERMISSION_ADMIN_ONLY.equals(settings.getSendMessagePermission())
                && (room.getAdmins() == null || !room.getAdmins().contains(userId))) {
            throw new AccessDeniedException("Only group admins can send messages");
        }
    }

    /**
     * Defensive check so callers cannot aggregate unread counts across arbitrary room ids.
     */
    private void ensurePrincipalMemberOfAllRooms(AuthUserPrincipal principal, Collection<String> roomIds) {
        List<String> distinctRoomIds = roomIds.stream().distinct().toList();
        List<Room> rooms = roomRepository.findAllById(distinctRoomIds);
        if (rooms.size() != distinctRoomIds.size()) {
            throw new AccessDeniedException("Forbidden");
        }
        String userId = principal.getId();
        for (Room room : rooms) {
            ensureMembership(room, userId);
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

    private List<Message> findUnreadMessages(String roomId, String actorUserId, int limit) {
        Criteria criteria = Criteria.where("roomId").is(roomId)
                .and("senderId").ne(actorUserId)
                .and("readByUserIds").ne(actorUserId)
                .and("deletedForUserIds").ne(actorUserId);

        return mongoTemplate.find(Query.query(criteria).limit(limit), Message.class);
    }

    private void notifyOfflineRecipients(Room room, String senderId, String preview, String messageId) {
        List<User> recipients = userRepository.findAllById(room.getMemberIds()).stream()
                .filter(user -> !user.getId().equals(senderId))
                .filter(user -> !presenceService.isUserOnline(user.getId()))
                .toList();
        if (recipients.isEmpty()) {
            return;
        }

        User sender = userRepository.findById(senderId).orElse(null);
        String senderDisplayName = sender == null
                ? "Someone"
                : (sender.getDisplayName() != null && !sender.getDisplayName().isBlank()
                ? sender.getDisplayName()
                : sender.getUsername());
        String roomName = resolveRoomDisplayName(room, senderDisplayName);
        String messagePreview = preview == null || preview.isBlank() ? "Sent you a message" : preview;

        recipients.forEach(recipient -> notificationService.createSystemNotification(
                recipient.getId(),
                NOTIFICATION_NEW_MESSAGE,
                "New message",
                senderDisplayName + " sent a message in " + roomName + ": " + messagePreview,
                room.getId()
        ));
    }

    private void cleanupStoredAttachment(StoredMessageAttachment storedAttachment) {
        try {
            messageAttachmentStorageService.delete(storedAttachment);
        } catch (RuntimeException cleanupException) {
            log.warn("Failed to cleanup orphan attachment: {}", storedAttachment.storagePublicId(), cleanupException);
        }
    }

    private String resolveRoomDisplayName(Room room, String fallback) {
        if (room.getName() != null && !room.getName().isBlank()) {
            return room.getName();
        }
        if ("direct".equalsIgnoreCase(room.getType())) {
            return "your chat";
        }
        if ("group".equalsIgnoreCase(room.getType())) {
            return "your group";
        }
        return fallback;
    }

    private MessageResponse createMessage(AuthUserPrincipal principal, CreateMessageRequest request) {
        Room room = ensureRoomExists(request.roomId());
        ensureMembership(room, principal.getId());
        ensureNotBlockedInDirectRoom(room, principal.getId());
        ensureCanSendInGroup(room, principal.getId());
        String normalizedContent = normalizeRequiredContent(request.content());
        String normalizedType = normalizeMessageType(request.type());
        Message replyToMessage = resolveReplyToMessage(request.replyToMessageId(), request.roomId());
        String normalizedClientMessageId = normalizeClientMessageId(request.clientMessageId());
        MessageResponse existing = findExistingClientMessage(request.roomId(), principal.getId(), normalizedClientMessageId);
        if (existing != null) {
            return existing;
        }
        if (!TYPE_TEXT.equals(normalizedType)) {
            throw new BadRequestException("Only TEXT messages can be created without an attachment");
        }
        LocalDateTime now = LocalDateTime.now();

        Message message = Message.builder()
                .roomId(request.roomId())
                .senderId(principal.getId())
                .content(normalizedContent)
                .type(normalizedType)
                .replyToMessageId(replyToMessage == null ? null : replyToMessage.getId())
                .clientMessageId(normalizedClientMessageId)
                .timestamp(now)
                .status("sent")
                .deliveredToUserIds(new HashSet<>(Set.of(principal.getId())))
                .readByUserIds(new HashSet<>(Set.of(principal.getId())))
                .updatedAt(now)
                .build();

        Message savedMessage = messageRepository.save(message);
        updateRoomLastMessage(room, message.getContent(), null);
        notifyOfflineRecipients(room, principal.getId(), savedMessage.getContent(), savedMessage.getId());
        MessageResponse response = replyToMessage == null
                ? messageMapper.toResponse(savedMessage, List.of())
                : messageMapper.toResponse(savedMessage, List.of(), replyToMessage);
        messagingTemplate.convertAndSend("/topic/rooms/" + request.roomId() + "/messages", response);
        return response;
    }

    private MessageResponse findExistingClientMessage(String roomId, String senderId, String clientMessageId) {
        if (clientMessageId == null) {
            return null;
        }
        return messageRepository.findByRoomIdAndSenderIdAndClientMessageId(roomId, senderId, clientMessageId)
                .map(message -> toResponseForUser(message, senderId))
                .orElse(null);
    }

    private MessageResponse updateMessageStatus(AuthUserPrincipal principal, String messageId, String status) {
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
        MessageResponse response = toResponseForUser(saved, principal.getId());
        messagingTemplate.convertAndSend("/topic/rooms/" + message.getRoomId() + "/status", response);
        return response;
    }

    @Override
    public String getRoomIdForMessage(String messageId) {
        return messageRepository.findById(messageId)
                .map(Message::getRoomId)
                .orElse(null);
    }
}
