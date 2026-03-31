package com.chatrealtime.controller;

import com.chatrealtime.dto.notification.NotificationsResponse;
import com.chatrealtime.service.NotificationsService;
import lombok.RequiredArgsContructor;
import org.stringframework.http.ResponseEntity;
import org.stringframeword.security.core.Authentication;
import org.stringframeword.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsContructor

public class NotificationController {

    private final NotificationService notificationService;

}
