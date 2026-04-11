package com.chatrealtime.modules.room.service;

import com.chatrealtime.modules.room.dto.CreateRoomRequest;
import com.chatrealtime.modules.room.dto.response.RoomResponse;
import com.chatrealtime.exception.BadRequestException;
import com.chatrealtime.exception.RoomNotFoundException;
import com.chatrealtime.modules.room.mapper.RoomMapper;
import com.chatrealtime.modules.room.model.Room;
import com.chatrealtime.modules.room.repository.RoomRepository;
import com.chatrealtime.modules.user.repository.UserRepository;
import com.chatrealtime.security.AuthContextService;
import com.chatrealtime.security.AuthUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoomService {
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final RoomMapper roomMapper;
    private final AuthContextService authContextService;

    public List<RoomResponse> getCurrentUserRooms() {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        return roomRepository.findByMemberIdsContaining(principal.getId())
                .stream()
                .map(roomMapper::toResponse)
                .toList();
    }

    public RoomResponse getRoomById(String roomId) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found"));
        if (room.getMemberIds() == null || !room.getMemberIds().contains(principal.getId())) {
            throw new BadRequestException("Current user is not a member of this room");
        }
        return roomMapper.toResponse(room);
    }

    public RoomResponse createRoom(CreateRoomRequest request) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        if (!"direct".equalsIgnoreCase(request.type()) && !"group".equalsIgnoreCase(request.type())) {
            throw new BadRequestException("room type must be direct or group");
        }

        Set<String> uniqueMemberIds = new LinkedHashSet<>(request.memberIds());
        uniqueMemberIds.add(principal.getId());
        if (uniqueMemberIds.size() < 2) {
            throw new BadRequestException("A room must have at least 2 members");
        }
        for (String memberId : uniqueMemberIds) {
            if (!userRepository.existsById(memberId)) {
                throw new BadRequestException("member does not exist: " + memberId);
            }
        }

        Instant now = Instant.now();
        Room room = Room.builder()
                .name(request.name().trim())
                .type(request.type().trim().toLowerCase(Locale.ROOT))
                .memberIds(List.copyOf(uniqueMemberIds))
                .createdBy(principal.getId())
                .createdAt(now)
                .updatedAt(now)
                .build();

        return roomMapper.toResponse(roomRepository.save(room));
    }

    public Room getRoomEntityById(String roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found"));
    }
}



