package com.chatrealtime.mapper;

import com.chatrealtime.dto.response.RoomResponse;
import com.chatrealtime.domain.Room;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoomMapper {
    RoomResponse toResponse(Room room);
}


