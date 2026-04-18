package com.chatrealtime.service.impl;

import com.chatrealtime.service.RoomService;

import com.chatrealtime.dto.request.CreateRoomRequest;
import com.chatrealtime.dto.response.RoomResponse;
import com.chatrealtime.exception.BadRequestException;
import com.chatrealtime.exception.RoomNotFoundException;
import com.chatrealtime.mapper.RoomMapper;
import com.chatrealtime.domain.Room;
import com.chatrealtime.repository.RoomRepository;
import com.chatrealtime.repository.UserRepository;
import com.chatrealtime.security.AuthContextService;
import com.chatrealtime.security.AuthUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final RoomMapper roomMapper;
    private final AuthContextService authContextService;

    @Override
    public List<RoomResponse> getCurrentUserRooms() {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        return roomRepository.findByMemberIdsContaining(principal.getId())
                .stream()
                .map(roomMapper::toResponse)
                .toList();
    }

    @Override
    public RoomResponse getRoomById(String roomId) {
        AuthUserPrincipal principal = authContextService.requireCurrentUser();
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found"));
        if (room.getMemberIds() == null || !room.getMemberIds().contains(principal.getId())) {
            throw new BadRequestException("Current user is not a member of this room");
        }
        return roomMapper.toResponse(room);
    }

    @Override
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

    @Override
    public Room getRoomEntityById(String roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found"));
    }
}



