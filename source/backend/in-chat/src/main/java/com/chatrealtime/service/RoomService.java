package com.chatrealtime.service;

import com.chatrealtime.domain.Room;
import com.chatrealtime.dto.request.CreateRoomRequest;
import com.chatrealtime.dto.response.RoomResponse;

import java.util.List;

public interface RoomService {
    List<RoomResponse> getCurrentUserRooms();

    RoomResponse getRoomById(String roomId);

    RoomResponse createRoom(CreateRoomRequest request);

    Room getRoomEntityById(String roomId);
}
