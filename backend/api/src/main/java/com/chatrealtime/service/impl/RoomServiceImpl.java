package com.chatrealtime.service.impl;

import com.chatrealtime.service.RoomService;

import com.chatrealtime.domain.Message;
import com.chatrealtime.dto.request.CreateRoomRequest;
import com.chatrealtime.dto.request.AddRoomMembersRequest;
import com.chatrealtime.dto.request.UpdateRoomNameRequest;
import com.chatrealtime.dto.response.RoomResponse;
import com.chatrealtime.exception.BadRequestException;
import com.chatrealtime.exception.RoomNotFoundException;
import com.chatrealtime.exception.UserNotFoundException;
import com.chatrealtime.mapper.RoomMapper;
import com.chatrealtime.domain.Room;
import com.chatrealtime.domain.User;
import com.chatrealtime.repository.MessageAttachmentRepository;
import com.chatrealtime.repository.MessageRepository;
import com.chatrealtime.repository.RoomRepository;
import com.chatrealtime.repository.UserRepository;
import com.chatrealtime.security.AuthContextService;
import com.chatrealtime.security.AuthUserPrincipal;
import com.chatrealtime.service.MessageService;
import com.chatrealtime.service.NotificationService;
import com.chatrealtime.storage.AvatarStorageService;
import com.chatrealtime.storage.AvatarUploadResult;
import lombok.RequiredArgsConstructor;
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
public class RoomServiceImpl implements RoomService {
    private static final String ROOM_TYPE_DIRECT = "direct";
    private static final String ROOM_TYPE_GROUP = "group";
    private static final String NOTIFICATION_GROUP_ADDED = "group_member_added";

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final RoomMapper roomMapper;
    private final AuthContextService authContextService;
    private final MessageService messageService;
    private final NotificationService notificationService;
    private final AvatarStorageService avatarStorageService;

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
            throw new BadRequestException("Current user is not a member of this room");
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
        if (ROOM_TYPE_DIRECT.equals(roomType)) {
            validateDirectRoomMembers(uniqueMemberIds);
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
                .memberIds(List.copyOf(uniqueMemberIds))
                .admins(List.of(principal.getId()))
                .createdBy(principal.getId())
                .ownerId(principal.getId())
                .lastMessageAt(null)
                .lastMessagePreview(null)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Room savedRoom = roomRepository.save(room);
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
    public RoomResponse updateRoomName(String roomId, UpdateRoomNameRequest request) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Room room = requireGroupRoomForMember(roomId, principal.getId());
        ensureAdmin(room, principal.getId());

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
        ensureAdmin(room, principal.getId());

        String oldAvatarProvider = room.getAvatarProvider();
        String oldAvatarPublicId = room.getAvatarPublicId();
        AvatarUploadResult uploadedAvatar = avatarStorageService.uploadAvatar("room-" + room.getId(), file);

        room.setAvatar(uploadedAvatar.url());
        room.setAvatarProvider(uploadedAvatar.provider());
        room.setAvatarPublicId(uploadedAvatar.publicId());
        room.setUpdatedAt(Instant.now());

        Room savedRoom = roomRepository.save(room);
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
            throw new BadRequestException("Only the room owner can dissolve this room");
        }
        deleteRoomData(room);
    }

    @Override
    public Room getRoomEntityById(String roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found"));
    }

    private Room requireGroupRoomForMember(String roomId, String userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found"));
        if (!ROOM_TYPE_GROUP.equals(room.getType())) {
            throw new BadRequestException("This operation is only supported for group rooms");
        }
        if (room.getMemberIds() == null || !room.getMemberIds().contains(userId)) {
            throw new BadRequestException("Current user is not a member of this room");
        }
        return room;
    }

    private void ensureAdmin(Room room, String userId) {
        if (!safeAdmins(room).contains(userId)) {
            throw new BadRequestException("Only room admins can perform this action");
        }
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
        for (String memberId : memberIds) {
            if (!userRepository.existsById(requireNonBlank(memberId, "memberId is required"))) {
                throw new BadRequestException("member does not exist: " + memberId);
            }
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
        List<String> memberIds = List.copyOf(uniqueMemberIds);
        return roomRepository.findByTypeAndMemberIdsContaining(ROOM_TYPE_DIRECT, memberIds.get(0))
                .stream()
                .filter(room -> room.getMemberIds() != null)
                .filter(room -> room.getMemberIds().size() == 2)
                .filter(room -> room.getMemberIds().contains(memberIds.get(1)))
                .findFirst();
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
        List<Message> messages = messageRepository.findByRoomId(room.getId());
        List<String> messageIds = messages.stream().map(Message::getId).toList();
        if (!messageIds.isEmpty()) {
            messageAttachmentRepository.deleteByMessageIdIn(messageIds);
            messageRepository.deleteAll(messages);
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
}



