package com.chatrealtime.client;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("notifications")
public interface NotificationClient {
    @PostExchange
    NotificationResponse send(@RequestBody NotificationRequest request);

    record NotificationRequest(String userId, String title, String message) {
    }

    record NotificationResponse(String id, String status) {
    }
}
