package com.chatrealtime.config;

import com.chatrealtime.realtime.NotificationRealtimeEventBus;
import com.chatrealtime.realtime.PresenceRealtimeEventBus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@Profile("!test")
@EnableConfigurationProperties(AppRedisProperties.class)
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true")
public class RedisConfig {
    @Bean
    public CacheManager redisCacheManager(
            RedisConnectionFactory redisConnectionFactory,
            AppRedisProperties redisProperties
    ) {
        RedisSerializationContext.SerializationPair<Object> serializer =
                RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer());
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(serializer);

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withCacheConfiguration(
                        "user-principals-by-id",
                        defaultCacheConfig.entryTtl(redisProperties.cache().userPrincipalByIdTtl())
                )
                .withCacheConfiguration(
                        "user-principals-by-username",
                        defaultCacheConfig.entryTtl(redisProperties.cache().userPrincipalByUsernameTtl())
                )
                .build();
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory redisConnectionFactory,
            AppRedisProperties redisProperties,
            PresenceRealtimeEventBus presenceRealtimeEventBus,
            NotificationRealtimeEventBus notificationRealtimeEventBus
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(
                presenceRealtimeEventBus::onRedisMessage,
                new PatternTopic(redisProperties.channels().presence())
        );
        container.addMessageListener(
                notificationRealtimeEventBus::onRedisMessage,
                new PatternTopic(redisProperties.channels().notification())
        );
        return container;
    }
}
