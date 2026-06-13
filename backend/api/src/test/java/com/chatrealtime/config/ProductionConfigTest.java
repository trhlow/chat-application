package com.chatrealtime.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionConfigTest {

    @Test
    void applicationProd_shouldConfigureRedisUnderSpringData() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application-prod.yml"));

        Properties properties = yaml.getObject();

        assertThat(properties)
                .containsEntry("spring.data.redis.host", "${SPRING_DATA_REDIS_HOST}")
                .containsEntry("spring.data.redis.port", "${SPRING_DATA_REDIS_PORT:6379}");
        assertThat(properties).doesNotContainKey("spring.mongodb.redis.host");
    }
}
