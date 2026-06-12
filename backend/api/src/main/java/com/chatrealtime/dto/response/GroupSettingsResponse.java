package com.chatrealtime.dto.response;

public record GroupSettingsResponse(
        String sendMessagePermission,
        String editGroupInfoPermission,
        String inviteMemberPermission,
        boolean allowNewMemberReadHistory
) {
}
