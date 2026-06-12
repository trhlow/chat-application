package com.chatrealtime.mapper;

import com.chatrealtime.domain.GroupSettings;
import com.chatrealtime.domain.Room;
import com.chatrealtime.dto.response.GroupSettingsResponse;
import com.chatrealtime.dto.response.RoomResponse;
import org.springframework.stereotype.Component;

@Component
public class RoomMapper {
    public RoomResponse toResponse(Room room) {
        return toResponse(room, 0L);
    }

    public RoomResponse toResponse(Room room, long unreadCount) {
        String avatarEndpoint = avatarEndpoint(room);
        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getType(),
                avatarEndpoint,
                avatarEndpoint,
                room.getMemberIds(),
                room.getAdmins(),
                room.getCreatedBy(),
                room.getOwnerId(),
                unreadCount,
                room.getLastMessageAt(),
                room.getLastMessagePreview(),
                room.getCreatedAt(),
                room.getUpdatedAt(),
                toSettingsResponse(room.getSettings())
        );
    }

    private String avatarEndpoint(Room room) {
        return room.getAvatar() == null || room.getAvatar().isBlank()
                ? null
                : "/api/rooms/" + room.getId() + "/avatar";
    }

    private GroupSettingsResponse toSettingsResponse(GroupSettings settings) {
        GroupSettings safeSettings = settings == null ? GroupSettings.defaults() : settings;
        return new GroupSettingsResponse(
                safeSettings.getSendMessagePermission(),
                safeSettings.getEditGroupInfoPermission(),
                safeSettings.getInviteMemberPermission(),
                safeSettings.isAllowNewMemberReadHistory()
        );
    }
}


