package com.chatrealtime.realtime;

import com.chatrealtime.dto.response.NotificationRealtimeEventResponse;

public record NotificationRealtimeDelivery(
        String destinationUsername,
        NotificationRealtimeEventResponse event
) {
}
