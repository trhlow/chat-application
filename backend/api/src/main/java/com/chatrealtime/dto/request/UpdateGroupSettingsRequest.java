package com.chatrealtime.dto.request;

import jakarta.validation.constraints.Pattern;

public record UpdateGroupSettingsRequest(
        @Pattern(regexp = "ALL|ADMIN_ONLY", message = "sendMessagePermission must be ALL or ADMIN_ONLY")
        String sendMessagePermission,

        @Pattern(regexp = "ALL|ADMIN_ONLY", message = "editGroupInfoPermission must be ALL or ADMIN_ONLY")
        String editGroupInfoPermission,

        @Pattern(regexp = "ALL|ADMIN_ONLY", message = "inviteMemberPermission must be ALL or ADMIN_ONLY")
        String inviteMemberPermission,

        Boolean allowNewMemberReadHistory
) {
}
