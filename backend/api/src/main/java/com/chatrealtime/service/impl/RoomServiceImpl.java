package com.chatrealtime.service.impl;

import com.chatrealtime.service.RoomService;

import com.chatrealtime.domain.GroupSettings;
import com.chatrealtime.domain.GroupJoinRequest;
import com.chatrealtime.domain.GroupJoinRequestStatus;
import com.chatrealtime.domain.Message;
import com.chatrealtime.dto.request.CreateRoomRequest;
import com.chatrealtime.dto.request.AddRoomMembersRequest;
import com.chatrealtime.dto.request.CreateGroupJoinRequest;
import com.chatrealtime.dto.request.UpdateGroupSettingsRequest;
import com.chatrealtime.dto.request.UpdateRoomNameRequest;
import com.chatrealtime.dto.response.GroupJoinRequestResponse;
import com.chatrealtime.dto.response.RoomMemberResponse;
import com.chatrealtime.dto.response.RoomResponse;
import com.chatrealtime.exception.BadRequestException;
import com.chatrealtime.exception.RoomNotFoundException;
import com.chatrealtime.exception.UserNotFoundException;
import com.chatrealtime.mapper.RoomMapper;
import com.chatrealtime.domain.Room;
import com.chatrealtime.domain.User;
import com.chatrealtime.repository.MessageAttachmentRepository;
import com.chatrealtime.repository.MessageRepository;
import com.chatrealtime.repository.GroupJoinRequestRepository;
import com.chatrealtime.repository.RoomRepository;
import com.chatrealtime.repository.UserRepository;
import com.chatrealtime.security.AuthContextService;
import com.chatrealtime.security.AuthUserPrincipal;
import com.chatrealtime.service.MessageService;
import com.chatrealtime.service.NotificationService;
import com.chatrealtime.storage.AvatarStorageService;
import com.chatrealtime.storage.AvatarUploadResult;
import com.chatrealtime.util.UserIdPair;
import com.chatrealtime.repository.UserBlockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class RoomServiceImpl implements RoomService {
    private static final String ROOM_TYPE_DIRECT = "direct";
    private static final String ROOM_TYPE_GROUP = "group";
    private static final String NOTIFICATION_GROUP_ADDED = "group_member_added";
    private static final String NOTIFICATION_GROUP_INVITE_REQUEST = "group_invite_request";
    private static final String NOTIFICATION_GROUP_INVITE_APPROVED = "group_invite_approved";
    private static final int ROOM_DELETE_MESSAGE_BATCH_SIZE = 500;

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final GroupJoinRequestRepository groupJoinRequestRepository;
    private final RoomMapper roomMapper;
    private final AuthContextService authContextService;
    private final MessageService messageService;
    private final NotificationService notificationService;
    private final AvatarStorageService avatarStorageService;
    private final MongoTemplate mongoTemplate;
    private final UserBlockRepository userBlockRepository;

    @Override
    public List<RoomResponse> getCurrentUserRooms() {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        List<Room> rooms = roomRepository.findByMemberIdsContaining(principal.getId())
                .stream()
                .sorted(Comparator
                        .comparing(Room::getLastMessageAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Room::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return mapRoomsWithUnreadCounts(rooms);
    }

    @Override
    public RoomResponse getRoomById(String roomId) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found"));
        if (room.getMemberIds() == null || !room.getMemberIds().contains(principal.getId())) {
            throw new AccessDeniedException("Forbidden");
        }
        long unreadCount = messageService.getUnreadCountMap(List.of(room.getId())).getOrDefault(room.getId(), 0L);
        return roomMapper.toResponse(room, unreadCount);
    }

    @Override
    public RoomResponse createRoom(CreateRoomRequest request) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        String roomType = normalizeRoomType(request.type());
        Set<String> uniqueMemberIds = normalizeMemberIds(request.memberIds());
        uniqueMemberIds.add(principal.getId());

        validateMembersExist(uniqueMemberIds);
        String directKey = null;
        if (ROOM_TYPE_DIRECT.equals(roomType)) {
            validateDirectRoomMembers(uniqueMemberIds);
            
            String otherUserId = uniqueMemberIds.stream()
                    .filter(id -> !id.equals(principal.getId()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("A direct room must contain exactly 2 distinct members"));
            ensureNotBlockedInDirectRoom(principal.getId(), otherUserId);

            directKey = directKey(uniqueMemberIds);
            Optional<Room> existingDirectRoom = findExistingDirectRoom(uniqueMemberIds);
            if (existingDirectRoom.isPresent()) {
                return roomMapper.toResponse(existingDirectRoom.get(), 0L);
            }
        } else {
            validateGroupRoomMembers(uniqueMemberIds, request.name());
        }

        Instant now = Instant.now();
        Room room = Room.builder()
                .name(normalizeName(request.name()))
                .type(roomType)
                .avatar(null)
                .avatarPublicId(null)
                .avatarProvider(null)
                .directKey(directKey)
                .memberIds(List.copyOf(uniqueMemberIds))
                .admins(List.of(principal.getId()))
                .settings(GroupSettings.defaults())
                .createdBy(principal.getId())
                .ownerId(principal.getId())
                .lastMessageAt(null)
                .lastMessagePreview(null)
                .createdAt(now)
                .updatedAt(now)
                .build();

        if (ROOM_TYPE_DIRECT.equals(roomType)) {
            Optional<Room> lateExistingDirectRoom = findExistingDirectRoom(uniqueMemberIds);
            if (lateExistingDirectRoom.isPresent()) {
                log.debug("Direct room appeared between checks; returning existing {}", lateExistingDirectRoom.get().getId());
                return roomMapper.toResponse(lateExistingDirectRoom.get(), 0L);
            }
        }

        Room savedRoom;
        try {
            savedRoom = roomRepository.save(room);
        } catch (DuplicateKeyException exception) {
            if (ROOM_TYPE_DIRECT.equals(roomType)) {
                return roomRepository.findByTypeAndDirectKey(ROOM_TYPE_DIRECT, directKey)
                        .map(existingRoom -> roomMapper.toResponse(existingRoom, 0L))
                        .orElseThrow(() -> exception);
            }
            throw exception;
        }
        if (ROOM_TYPE_GROUP.equals(roomType)) {
            notifyAddedMembers(savedRoom, uniqueMemberIds.stream()
                    .filter(memberId -> !memberId.equals(principal.getId()))
                    .toList());
        }
        return roomMapper.toResponse(savedRoom, 0L);
    }

    @Override
    public RoomResponse addMembers(String roomId, AddRoomMembersRequest request) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Room room = requireGroupRoomForMember(roomId, principal.getId());
        ensureAdmin(room, principal.getId());

        LinkedHashSet<String> updatedMembers = new LinkedHashSet<>(room.getMemberIds());
        List<String> newMemberIds = request.memberIds().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(memberId -> !memberId.isBlank())
                .filter(memberId -> !updatedMembers.contains(memberId))
                .toList();
        if (newMemberIds.isEmpty()) {
            throw new BadRequestException("No new members to add");
        }
        validateMembersExist(new LinkedHashSet<>(newMemberIds));

        updatedMembers.addAll(newMemberIds);
        room.setMemberIds(List.copyOf(updatedMembers));
        room.setUpdatedAt(Instant.now());

        Room savedRoom = roomRepository.save(room);
        notifyAddedMembers(savedRoom, newMemberIds);
        long unreadCount = messageService.getUnreadCountMap(List.of(savedRoom.getId())).getOrDefault(savedRoom.getId(), 0L);
        return roomMapper.toResponse(savedRoom, unreadCount);
    }

    @Override
    public RoomResponse removeMember(String roomId, String memberId) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Room room = requireGroupRoomForMember(roomId, principal.getId());
        ensureAdmin(room, principal.getId());

        String normalizedMemberId = requireNonBlank(memberId, "memberId is required");
        if (!room.getMemberIds().contains(normalizedMemberId)) {
            throw new BadRequestException("Member is not in this room");
        }
        if (normalizedMemberId.equals(room.getOwnerId())) {
            throw new BadRequestException("Owner cannot be removed from the room");
        }

        room.setMemberIds(room.getMemberIds().stream()
                .filter(existingMemberId -> !existingMemberId.equals(normalizedMemberId))
                .toList());
        room.setAdmins(safeAdmins(room).stream()
                .filter(adminId -> !adminId.equals(normalizedMemberId))
                .toList());
        room.setUpdatedAt(Instant.now());

        Room savedRoom = roomRepository.save(room);
        long unreadCount = messageService.getUnreadCountMap(List.of(savedRoom.getId())).getOrDefault(savedRoom.getId(), 0L);
        return roomMapper.toResponse(savedRoom, unreadCount);
    }

    @Override
    public List<RoomMemberResponse> getMembers(String roomId) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Room room = requireGroupRoomForMember(roomId, principal.getId());
        Map<String, User> usersById = userRepository.findAllById(room.getMemberIds())
                .stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, user -> user));
        return room.getMemberIds().stream()
                .map(memberId -> toRoomMemberResponse(room, usersById.get(memberId)))
                .toList();
    }

    @Override
    public RoomResponse promoteAdmin(String roomId, String memberId) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Room room = requireGroupRoomForMember(roomId, principal.getId());
        ensureOwner(room, principal.getId());

        String normalizedMemberId = requireExistingMember(room, memberId);
        room.setAdmins(mergePreservingOrder(safeAdmins(room), List.of(normalizedMemberId)));
        room.setUpdatedAt(Instant.now());
        Room savedRoom = roomRepository.save(room);
        return roomMapper.toResponse(savedRoom, unreadCount(savedRoom));
    }

    @Override
    public RoomResponse demoteAdmin(String roomId, String memberId) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Room room = requireGroupRoomForMember(roomId, principal.getId());
        ensureOwner(room, principal.getId());

        String normalizedMemberId = requireExistingMember(room, memberId);
        if (normalizedMemberId.equals(room.getOwnerId())) {
            throw new BadRequestException("Owner cannot be demoted");
        }
        room.setAdmins(safeAdmins(room).stream()
                .filter(adminId -> !adminId.equals(normalizedMemberId))
                .toList());
        room.setUpdatedAt(Instant.now());
        Room savedRoom = roomRepository.save(room);
        return roomMapper.toResponse(savedRoom, unreadCount(savedRoom));
    }

    @Override
    public RoomResponse transferOwner(String roomId, String memberId) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Room room = requireGroupRoomForMember(roomId, principal.getId());
        ensureOwner(room, principal.getId());

        String newOwnerId = requireExistingMember(room, memberId);
        room.setOwnerId(newOwnerId);
        room.setAdmins(mergePreservingOrder(safeAdmins(room), List.of(newOwnerId)));
        room.setUpdatedAt(Instant.now());
        Room savedRoom = roomRepository.save(room);
        return roomMapper.toResponse(savedRoom, unreadCount(savedRoom));
    }

    @Override
    public RoomResponse updateGroupSettings(String roomId, UpdateGroupSettingsRequest request) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Room room = requireGroupRoomForMember(roomId, principal.getId());
        ensureOwner(room, principal.getId());

        GroupSettings settings = groupSettings(room);
        if (request.sendMessagePermission() != null) {
            settings.setSendMessagePermission(normalizePermission(request.sendMessagePermission(), "sendMessagePermission"));
        }
        if (request.editGroupInfoPermission() != null) {
            settings.setEditGroupInfoPermission(normalizePermission(request.editGroupInfoPermission(), "editGroupInfoPermission"));
        }
        if (request.inviteMemberPermission() != null) {
            settings.setInviteMemberPermission(normalizePermission(request.inviteMemberPermission(), "inviteMemberPermission"));
        }
        if (request.allowNewMemberReadHistory() != null) {
            settings.setAllowNewMemberReadHistory(request.allowNewMemberReadHistory());
        }
        room.setSettings(settings);
        room.setUpdatedAt(Instant.now());
        Room savedRoom = roomRepository.save(room);
        return roomMapper.toResponse(savedRoom, unreadCount(savedRoom));
    }

    @Override
    public GroupJoinRequestResponse requestMemberInvite(String roomId, CreateGroupJoinRequest request) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Room room = requireGroupRoomForMember(roomId, principal.getId());
        ensureCanRequestInvite(room, principal.getId());

        String targetUserId = requireNonBlank(request.targetUserId(), "targetUserId is required");
        if (room.getMemberIds().contains(targetUserId)) {
            throw new BadRequestException("User is already a member");
        }
        validateMembersExist(Set.of(targetUserId));
        if (groupJoinRequestRepository.existsByRoomIdAndTargetUserIdAndStatus(
                room.getId(),
                targetUserId,
                GroupJoinRequestStatus.PENDING
        )) {
            throw new BadRequestException("A pending group join request already exists");
        }

        GroupJoinRequest savedRequest = groupJoinRequestRepository.save(GroupJoinRequest.builder()
                .roomId(room.getId())
                .requesterId(principal.getId())
                .targetUserId(targetUserId)
                .status(GroupJoinRequestStatus.PENDING)
                .createdAt(Instant.now())
                .build());

        notifyAdminsOfInviteRequest(room, principal.getId(), targetUserId, savedRequest.getId());
        return toGroupJoinRequestResponse(savedRequest);
    }

    @Override
    public List<GroupJoinRequestResponse> getPendingJoinRequests(String roomId) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Room room = requireGroupRoomForMember(roomId, principal.getId());
        ensureAdmin(room, principal.getId());
        return groupJoinRequestRepository.findByRoomIdAndStatusOrderByCreatedAtDesc(room.getId(), GroupJoinRequestStatus.PENDING)
                .stream()
                .map(this::toGroupJoinRequestResponse)
                .toList();
    }

    @Override
    public RoomResponse approveJoinRequest(String roomId, String requestId) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Room room = requireGroupRoomForMember(roomId, principal.getId());
        ensureAdmin(room, principal.getId());
        GroupJoinRequest joinRequest = requirePendingJoinRequest(room.getId(), requestId);
        if (room.getMemberIds().contains(joinRequest.getTargetUserId())) {
            markJoinRequest(joinRequest, GroupJoinRequestStatus.APPROVED, principal.getId());
            return roomMapper.toResponse(room, unreadCount(room));
        }

        room.setMemberIds(mergePreservingOrder(room.getMemberIds(), List.of(joinRequest.getTargetUserId())));
        room.setUpdatedAt(Instant.now());
        Room savedRoom = roomRepository.save(room);
        markJoinRequest(joinRequest, GroupJoinRequestStatus.APPROVED, principal.getId());
        notifyAddedMembers(savedRoom, List.of(joinRequest.getTargetUserId()));
        notificationService.createSystemNotification(
                joinRequest.getTargetUserId(),
                NOTIFICATION_GROUP_INVITE_APPROVED,
                "Group invite approved",
                "You were added to group " + (savedRoom.getName() == null ? "group" : savedRoom.getName()),
                savedRoom.getId()
        );
        return roomMapper.toResponse(savedRoom, unreadCount(savedRoom));
    }

    @Override
    public GroupJoinRequestResponse rejectJoinRequest(String roomId, String requestId) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Room room = requireGroupRoomForMember(roomId, principal.getId());
        ensureAdmin(room, principal.getId());
        GroupJoinRequest joinRequest = requirePendingJoinRequest(room.getId(), requestId);
        return toGroupJoinRequestResponse(markJoinRequest(joinRequest, GroupJoinRequestStatus.REJECTED, principal.getId()));
    }

    @Override
    public RoomResponse updateRoomName(String roomId, UpdateRoomNameRequest request) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Room room = requireGroupRoomForMember(roomId, principal.getId());
        ensureCanEditGroupInfo(room, principal.getId());

        room.setName(normalizeRequiredGroupName(request.name()));
        room.setUpdatedAt(Instant.now());
        Room savedRoom = roomRepository.save(room);
        long unreadCount = messageService.getUnreadCountMap(List.of(savedRoom.getId())).getOrDefault(savedRoom.getId(), 0L);
        return roomMapper.toResponse(savedRoom, unreadCount);
    }

    @Override
    public RoomResponse updateRoomAvatar(String roomId, MultipartFile file) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Room room = requireGroupRoomForMember(roomId, principal.getId());
        ensureCanEditGroupInfo(room, principal.getId());

        String oldAvatarProvider = room.getAvatarProvider();
        String oldAvatarPublicId = room.getAvatarPublicId();
        AvatarUploadResult uploadedAvatar = avatarStorageService.uploadAvatar("room-" + room.getId(), file);

        room.setAvatar(uploadedAvatar.url());
        room.setAvatarProvider(uploadedAvatar.provider());
        room.setAvatarPublicId(uploadedAvatar.publicId());
        room.setUpdatedAt(Instant.now());

        final Room savedRoom;
        try {
            savedRoom = roomRepository.save(room);
        } catch (RuntimeException exception) {
            avatarStorageService.deleteAvatar(uploadedAvatar.provider(), uploadedAvatar.publicId());
            throw exception;
        }

        avatarStorageService.deleteAvatar(oldAvatarProvider, oldAvatarPublicId);
        long unreadCount = messageService.getUnreadCountMap(List.of(savedRoom.getId())).getOrDefault(savedRoom.getId(), 0L);
        return roomMapper.toResponse(savedRoom, unreadCount);
    }

    @Override
    public void leaveRoom(String roomId) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Room room = requireGroupRoomForMember(roomId, principal.getId());

        List<String> remainingMembers = room.getMemberIds().stream()
                .filter(memberId -> !memberId.equals(principal.getId()))
                .toList();
        if (remainingMembers.isEmpty()) {
            deleteRoomData(room);
            return;
        }

        room.setMemberIds(remainingMembers);
        room.setAdmins(safeAdmins(room).stream()
                .filter(adminId -> !adminId.equals(principal.getId()))
                .toList());
        if (principal.getId().equals(room.getOwnerId())) {
            String newOwnerId = room.getAdmins().isEmpty()
                    ? remainingMembers.get(0)
                    : room.getAdmins().get(0);
            room.setOwnerId(newOwnerId);
            if (!room.getAdmins().contains(newOwnerId)) {
                room.setAdmins(mergePreservingOrder(room.getAdmins(), List.of(newOwnerId)));
            }
        }

        room.setUpdatedAt(Instant.now());
        roomRepository.save(room);
    }

    @Override
    public void dissolveRoom(String roomId) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Room room = requireGroupRoomForMember(roomId, principal.getId());
        if (!principal.getId().equals(room.getOwnerId())) {
            throw new AccessDeniedException("Forbidden");
        }
        deleteRoomData(room);
    }

    @Override
    public Room getRoomEntityById(String roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found"));
    }

    private void ensureNotBlockedInDirectRoom(String userId, String otherUserId) {
        if (userBlockRepository.existsBetweenUsers(userId, otherUserId)) {
            throw new AccessDeniedException("Blocked users cannot create direct rooms");
        }
    }

    private Room requireGroupRoomForMember(String roomId, String userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found"));
        if (room.getMemberIds() == null || !room.getMemberIds().contains(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Forbidden");
        }
        if (!ROOM_TYPE_GROUP.equals(room.getType())) {
            throw new org.springframework.security.access.AccessDeniedException("This operation is only supported for group rooms");
        }
        return room;
    }

    private void ensureAdmin(Room room, String userId) {
        if (!safeAdmins(room).contains(userId)) {
            throw new AccessDeniedException("Forbidden");
        }
    }

    private void ensureOwner(Room room, String userId) {
        if (!userId.equals(room.getOwnerId())) {
            throw new AccessDeniedException("Forbidden");
        }
    }

    private void ensureCanRequestInvite(Room room, String userId) {
        GroupSettings settings = groupSettings(room);
        if (GroupSettings.PERMISSION_ADMIN_ONLY.equals(settings.getInviteMemberPermission())) {
            ensureAdmin(room, userId);
        }
    }

    private void ensureCanEditGroupInfo(Room room, String userId) {
        GroupSettings settings = groupSettings(room);
        if (GroupSettings.PERMISSION_ALL.equals(settings.getEditGroupInfoPermission())) {
            return;
        }
        ensureAdmin(room, userId);
    }

    private String normalizeRoomType(String type) {
        if (type == null || type.isBlank()) {
            throw new BadRequestException("room type is required");
        }
        String normalizedType = type.trim().toLowerCase(Locale.ROOT);
        if (!ROOM_TYPE_DIRECT.equals(normalizedType) && !ROOM_TYPE_GROUP.equals(normalizedType)) {
            throw new BadRequestException("room type must be direct or group");
        }
        return normalizedType;
    }

    private void validateMembersExist(Set<String> memberIds) {
        if (memberIds.isEmpty()) {
            throw new BadRequestException("memberIds is required");
        }
        memberIds.forEach(id -> requireNonBlank(id, "memberId is required"));
        long existingCount = userRepository.countByIdIn(memberIds);
        if (existingCount != memberIds.size()) {
            throw new BadRequestException("One or more members do not exist");
        }
    }

    private void validateDirectRoomMembers(Set<String> uniqueMemberIds) {
        if (uniqueMemberIds.size() != 2) {
            throw new BadRequestException("A direct room must contain exactly 2 members");
        }
    }

    private void validateGroupRoomMembers(Set<String> uniqueMemberIds, String name) {
        if (uniqueMemberIds.size() < 3) {
            throw new BadRequestException("A group room must have at least 3 members");
        }
        normalizeRequiredGroupName(name);
    }

    private Optional<Room> findExistingDirectRoom(Set<String> uniqueMemberIds) {
        String directKey = directKey(uniqueMemberIds);
        Optional<Room> byDirectKey = roomRepository.findByTypeAndDirectKey(ROOM_TYPE_DIRECT, directKey);
        if (byDirectKey.isPresent()) {
            return byDirectKey;
        }
        List<String> memberIds = List.copyOf(uniqueMemberIds);
        return roomRepository.findByTypeAndMemberIdsContaining(ROOM_TYPE_DIRECT, memberIds.get(0))
                .stream()
                .filter(room -> room.getMemberIds() != null)
                .filter(room -> room.getMemberIds().size() == 2)
                .filter(room -> room.getMemberIds().contains(memberIds.get(1)))
                .findFirst();
    }

    private String directKey(Set<String> uniqueMemberIds) {
        List<String> memberIds = List.copyOf(uniqueMemberIds);
        UserIdPair.Ordered pair = UserIdPair.order(memberIds.get(0), memberIds.get(1));
        return pair.userIdA() + ":" + pair.userIdB();
    }

    private String normalizeName(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeRequiredGroupName(String name) {
        String normalized = normalizeName(name);
        if (normalized == null) {
            throw new BadRequestException("Group name is required");
        }
        return normalized;
    }

    private void notifyAddedMembers(Room room, List<String> memberIds) {
        if (memberIds.isEmpty()) {
            return;
        }
        User actor = getCurrentUser();
        String actorDisplayName = displayName(actor);
        String roomName = room.getName() != null ? room.getName() : "group";
        memberIds.forEach(memberId -> notificationService.createSystemNotification(
                memberId,
                NOTIFICATION_GROUP_ADDED,
                "Added to group",
                actorDisplayName + " added you to group " + roomName,
                room.getId()
        ));
    }

    private void deleteRoomData(Room room) {
        while (true) {
            Query batchQuery = Query.query(Criteria.where("roomId").is(room.getId()))
                    .limit(ROOM_DELETE_MESSAGE_BATCH_SIZE);
            batchQuery.fields().include("_id");

            List<String> messageIds = mongoTemplate.find(batchQuery, Message.class)
                    .stream()
                    .map(Message::getId)
                    .toList();
            if (messageIds.isEmpty()) {
                break;
            }
            messageAttachmentRepository.deleteByMessageIdIn(messageIds);
            mongoTemplate.remove(Query.query(Criteria.where("_id").in(messageIds)), Message.class);
        }
        avatarStorageService.deleteAvatar(room.getAvatarProvider(), room.getAvatarPublicId());
        roomRepository.delete(room);
    }

    private List<RoomResponse> mapRoomsWithUnreadCounts(List<Room> rooms) {
        Map<String, Long> unreadCountMap = messageService.getUnreadCountMap(
                rooms.stream().map(Room::getId).toList()
        );
        return rooms.stream()
                .map(room -> roomMapper.toResponse(room, unreadCountMap.getOrDefault(room.getId(), 0L)))
                .toList();
    }

    private User getCurrentUser() {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new UserNotFoundException("Current user not found"));
    }

    private String displayName(User user) {
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            return user.getDisplayName();
        }
        return user.getUsername();
    }

    private List<String> safeAdmins(Room room) {
        return room.getAdmins() == null ? List.of() : room.getAdmins();
    }

    private GroupSettings groupSettings(Room room) {
        if (room.getSettings() == null) {
            room.setSettings(GroupSettings.defaults());
        }
        return room.getSettings();
    }

    private String normalizePermission(String permission, String fieldName) {
        if (permission == null || permission.isBlank()) {
            throw new BadRequestException(fieldName + " is required");
        }
        String normalized = permission.trim().toUpperCase(Locale.ROOT);
        if (!GroupSettings.PERMISSION_ALL.equals(normalized)
                && !GroupSettings.PERMISSION_ADMIN_ONLY.equals(normalized)) {
            throw new BadRequestException(fieldName + " must be ALL or ADMIN_ONLY");
        }
        return normalized;
    }

    private String requireExistingMember(Room room, String memberId) {
        String normalizedMemberId = requireNonBlank(memberId, "memberId is required");
        if (room.getMemberIds() == null || !room.getMemberIds().contains(normalizedMemberId)) {
            throw new BadRequestException("Member is not in this room");
        }
        return normalizedMemberId;
    }

    private long unreadCount(Room room) {
        return messageService.getUnreadCountMap(List.of(room.getId())).getOrDefault(room.getId(), 0L);
    }

    private RoomMemberResponse toRoomMemberResponse(Room room, User user) {
        if (user == null) {
            throw new UserNotFoundException("Room member not found");
        }
        String role = user.getId().equals(room.getOwnerId())
                ? "OWNER"
                : safeAdmins(room).contains(user.getId()) ? "ADMIN" : "MEMBER";
        String avatarEndpoint = user.getAvatar() == null || user.getAvatar().isBlank()
                ? null
                : "/api/users/" + user.getId() + "/avatar";
        return new RoomMemberResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                avatarEndpoint,
                avatarEndpoint,
                role
        );
    }

    private GroupJoinRequest requirePendingJoinRequest(String roomId, String requestId) {
        GroupJoinRequest joinRequest = groupJoinRequestRepository.findByIdAndRoomId(
                        requireNonBlank(requestId, "requestId is required"),
                        roomId
                )
                .orElseThrow(() -> new BadRequestException("Group join request not found"));
        if (joinRequest.getStatus() != GroupJoinRequestStatus.PENDING) {
            throw new BadRequestException("Group join request is not pending");
        }
        return joinRequest;
    }

    private GroupJoinRequest markJoinRequest(
            GroupJoinRequest joinRequest,
            GroupJoinRequestStatus status,
            String respondedBy
    ) {
        joinRequest.setStatus(status);
        joinRequest.setRespondedBy(respondedBy);
        joinRequest.setRespondedAt(Instant.now());
        return groupJoinRequestRepository.save(joinRequest);
    }

    private GroupJoinRequestResponse toGroupJoinRequestResponse(GroupJoinRequest request) {
        return new GroupJoinRequestResponse(
                request.getId(),
                request.getRoomId(),
                request.getRequesterId(),
                request.getTargetUserId(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getRespondedAt(),
                request.getRespondedBy()
        );
    }

    private void notifyAdminsOfInviteRequest(Room room, String requesterId, String targetUserId, String requestId) {
        User requester = userRepository.findById(requesterId).orElse(null);
        User target = userRepository.findById(targetUserId).orElse(null);
        String requesterName = requester == null ? "A member" : displayName(requester);
        String targetName = target == null ? "a user" : displayName(target);
        String roomName = room.getName() == null ? "group" : room.getName();
        safeAdmins(room).stream()
                .filter(adminId -> !adminId.equals(requesterId))
                .forEach(adminId -> notificationService.createSystemNotification(
                        adminId,
                        NOTIFICATION_GROUP_INVITE_REQUEST,
                        "Group invite needs approval",
                        requesterName + " invited " + targetName + " to " + roomName,
                        requestId
                ));
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value.trim();
    }

    private Set<String> normalizeMemberIds(List<String> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            throw new BadRequestException("memberIds is required");
        }
        return memberIds.stream()
                .map(memberId -> requireNonBlank(memberId, "memberId is required"))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private List<String> mergePreservingOrder(List<String> first, List<String> second) {
        LinkedHashSet<String> merged = new LinkedHashSet<>(first);
        merged.addAll(second);
        return List.copyOf(merged);
    }

    @Override
    public boolean isMember(String roomId, String userId) {
        return roomRepository.findById(roomId)
                .map(room -> room.getMemberIds() != null && room.getMemberIds().contains(userId))
                .orElse(false);
    }
}



