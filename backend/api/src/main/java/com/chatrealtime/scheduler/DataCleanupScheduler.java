package com.chatrealtime.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataCleanupScheduler {
    @Scheduled(cron = "${app.cleanup.cron:0 0 3 * * *}")
    public void cleanupExpiredData() {
        log.debug("Scheduled cleanup tick");
    }
}
