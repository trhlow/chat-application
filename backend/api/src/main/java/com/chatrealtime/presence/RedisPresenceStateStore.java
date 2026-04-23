package com.chatrealtime.presence;

import com.chatrealtime.config.AppRedisProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

@Component
@Profile("!test")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true")
public class RedisPresenceStateStore implements PresenceStateStore {
    private static final String ONLINE_VALUE = "online";

    private final StringRedisTemplate stringRedisTemplate;
    private final AppRedisProperties redisProperties;

    @Override
    public void markOnline(String userId) {
        stringRedisTemplate.opsForValue().set(
                key(userId),
                ONLINE_VALUE,
                redisProperties.presenceTtl()
        );
    }

    @Override
    public void markOffline(String userId) {
        stringRedisTemplate.delete(key(userId));
    }

    @Override
    public boolean isOnline(String userId) {
        Boolean hasKey = stringRedisTemplate.hasKey(key(userId));
        return Boolean.TRUE.equals(hasKey);
    }

    private String key(String userId) {
        return "presence:user:" + userId;
    }
}
