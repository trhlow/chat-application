package com.chatrealtime.security;

import java.time.Duration;

public interface RateLimitCounterStore {
    long incrementAndGet(String key, Duration window);
}
