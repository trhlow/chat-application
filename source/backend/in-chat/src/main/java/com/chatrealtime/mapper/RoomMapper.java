package com.chatrealtime.mapper;

import com.chatrealtime.dto.room.response.RoomResponse;
import com.chatrealtime.model.Room;
import org.springframework.stereotype.Component;

@Component
public class RoomMapper {
    public RoomResponse toResponse(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getType(),
                room.getMemberIds(),
                room.getCreatedBy(),
                room.getCreatedAt(),
                room.getUpdatedAt()
        );
    }
}
