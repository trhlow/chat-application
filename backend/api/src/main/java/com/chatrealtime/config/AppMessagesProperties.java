package com.chatrealtime.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Limits for message read-receipt processing.
 */
@ConfigurationProperties(prefix = "app.messages")
public record AppMessagesProperties(int markReadMaxBatches) {
    public AppMessagesProperties {
        if (markReadMaxBatches < 1) {
            throw new IllegalArgumentException("app.messages.mark-read-max-batches must be at least 1");
        }
    }
}
