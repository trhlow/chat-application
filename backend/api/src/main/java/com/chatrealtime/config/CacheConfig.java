package com.chatrealtime.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    @ConditionalOnProperty(
            prefix = "app.redis",
            name = "enabled",
            havingValue = "false",
            matchIfMissing = true
    )
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                "users",
                "rooms",
                "messages",
                "user-principals-by-id",
                "user-principals-by-username"
        );
    }
}
