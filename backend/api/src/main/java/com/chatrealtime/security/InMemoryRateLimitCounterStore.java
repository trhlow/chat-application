package com.chatrealtime.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(prefix = "app.rate-limit", name = "distributed", havingValue = "false", matchIfMissing = true)
public class InMemoryRateLimitCounterStore implements RateLimitCounterStore {
    private final Map<String, FixedWindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    public long incrementAndGet(String key, Duration window) {
        long now = System.currentTimeMillis();
        long windowMillis = window.toMillis();
        FixedWindowCounter counter = counters.computeIfAbsent(key, ignored -> new FixedWindowCounter(now));
        synchronized (counter) {
            if (now - counter.windowStartMillis >= windowMillis) {
                counter.windowStartMillis = now;
                counter.count = 0;
            }
            counter.count++;
            return counter.count;
        }
    }

    private static class FixedWindowCounter {
        private long windowStartMillis;
        private long count;

        private FixedWindowCounter(long windowStartMillis) {
            this.windowStartMillis = windowStartMillis;
        }
    }
}
