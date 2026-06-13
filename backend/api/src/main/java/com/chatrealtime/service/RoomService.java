package com.chatrealtime.service;

import com.chatrealtime.domain.Room;
import com.chatrealtime.dto.request.AddRoomMembersRequest;
import com.chatrealtime.dto.request.CreateGroupJoinRequest;
import com.chatrealtime.dto.request.CreateRoomRequest;
import com.chatrealtime.dto.request.UpdateGroupSettingsRequest;
import com.chatrealtime.dto.request.UpdateRoomNameRequest;
import com.chatrealtime.dto.response.GroupJoinRequestResponse;
import com.chatrealtime.dto.response.RoomMemberResponse;
import com.chatrealtime.dto.response.RoomResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface RoomService {
    List<RoomResponse> getCurrentUserRooms();

    RoomResponse getRoomById(String roomId);

    RoomResponse createRoom(CreateRoomRequest request);

    RoomResponse addMembers(String roomId, AddRoomMembersRequest request);

    RoomResponse removeMember(String roomId, String memberId);

    List<RoomMemberResponse> getMembers(String roomId);

    RoomResponse promoteAdmin(String roomId, String memberId);

    RoomResponse demoteAdmin(String roomId, String memberId);

    RoomResponse transferOwner(String roomId, String memberId);

    RoomResponse updateGroupSettings(String roomId, UpdateGroupSettingsRequest request);

    GroupJoinRequestResponse requestMemberInvite(String roomId, CreateGroupJoinRequest request);

    List<GroupJoinRequestResponse> getPendingJoinRequests(String roomId);

    RoomResponse approveJoinRequest(String roomId, String requestId);

    GroupJoinRequestResponse rejectJoinRequest(String roomId, String requestId);

    RoomResponse updateRoomName(String roomId, UpdateRoomNameRequest request);

    RoomResponse updateRoomAvatar(String roomId, MultipartFile file);

    void leaveRoom(String roomId);

    void dissolveRoom(String roomId);

    Room getRoomEntityById(String roomId);
}
