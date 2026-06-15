package com.chatrealtime.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Limits for message read-receipt processing.
 */
@ConfigurationProperties(prefix = "app.messages")
public record AppMessagesProperties(int markReadMaxBatches, boolean enforceRecallTimeLimit, int recallTimeLimitMinutes) {
    public AppMessagesProperties {
        if (markReadMaxBatches < 1) {
            throw new IllegalArgumentException("app.messages.mark-read-max-batches must be at least 1");
        }
        if (recallTimeLimitMinutes < 1) {
            throw new IllegalArgumentException("app.messages.recall-time-limit-minutes must be at least 1");
        }
    }
}
