package com.chatrealtime.modules.room.mapper;

import com.chatrealtime.modules.room.dto.response.RoomResponse;
import com.chatrealtime.modules.room.model.Room;
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


