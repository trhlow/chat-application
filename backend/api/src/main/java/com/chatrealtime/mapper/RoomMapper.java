package com.chatrealtime.mapper;

import com.chatrealtime.domain.Room;
import com.chatrealtime.dto.response.RoomResponse;
import org.springframework.stereotype.Component;

@Component
public class RoomMapper {
    public RoomResponse toResponse(Room room) {
        return toResponse(room, 0L);
    }

    public RoomResponse toResponse(Room room, long unreadCount) {
        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getType(),
                room.getAvatar(),
                room.getAvatarProvider(),
                room.getMemberIds(),
                room.getAdmins(),
                room.getCreatedBy(),
                room.getOwnerId(),
                unreadCount,
                room.getLastMessageAt(),
                room.getLastMessagePreview(),
                room.getCreatedAt(),
                room.getUpdatedAt()
        );
    }
}


