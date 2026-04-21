package com.chatrealtime.service;

import com.chatrealtime.domain.Room;
import com.chatrealtime.dto.request.AddRoomMembersRequest;
import com.chatrealtime.dto.request.CreateRoomRequest;
import com.chatrealtime.dto.request.UpdateRoomNameRequest;
import com.chatrealtime.dto.response.RoomResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface RoomService {
    List<RoomResponse> getCurrentUserRooms();

    RoomResponse getRoomById(String roomId);

    RoomResponse createRoom(CreateRoomRequest request);

    RoomResponse addMembers(String roomId, AddRoomMembersRequest request);

    RoomResponse removeMember(String roomId, String memberId);

    RoomResponse updateRoomName(String roomId, UpdateRoomNameRequest request);

    RoomResponse updateRoomAvatar(String roomId, MultipartFile file);

    void leaveRoom(String roomId);

    void dissolveRoom(String roomId);

    Room getRoomEntityById(String roomId);
}
