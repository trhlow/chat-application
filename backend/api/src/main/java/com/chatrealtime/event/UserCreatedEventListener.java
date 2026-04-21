package com.chatrealtime.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserCreatedEventListener {
    @Async
    @EventListener
    public void onUserCreated(UserCreatedEvent event) {
        log.debug("User created: {}", event.userId());
    }
}
