package com.chatrealtime.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupSettings {
    public static final String PERMISSION_ALL = "ALL";
    public static final String PERMISSION_ADMIN_ONLY = "ADMIN_ONLY";

    @Builder.Default
    private String sendMessagePermission = PERMISSION_ALL;
    @Builder.Default
    private String editGroupInfoPermission = PERMISSION_ADMIN_ONLY;
    @Builder.Default
    private String inviteMemberPermission = PERMISSION_ALL;
    @Builder.Default
    private boolean allowNewMemberReadHistory = true;

    public static GroupSettings defaults() {
        return GroupSettings.builder().build();
    }
}
