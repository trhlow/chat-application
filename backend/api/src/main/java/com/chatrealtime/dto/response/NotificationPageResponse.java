package com.chatrealtime.dto.response;

import java.util.List;

public record NotificationPageResponse(
        List<NotificationsResponse> items,
        int page,
        int size,
        boolean hasMore
) {
}
