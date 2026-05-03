package com.chatrealtime.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.rate-limit", name = "distributed", havingValue = "true")
public class RedisRateLimitCounterStore implements RateLimitCounterStore {
    private final StringRedisTemplate redisTemplate;

    @Override
    public long incrementAndGet(String key, Duration window) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            throw new IllegalStateException("Could not increment rate limit counter");
        }
        if (count == 1L) {
            redisTemplate.expire(key, window);
        }
        return count;
    }
}
