package com.chatrealtime.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ApiVersionConfig.ApiVersionProperties.class)
public class ApiVersionConfig {
    @ConfigurationProperties(prefix = "spring.mvc.apiversion")
    public record ApiVersionProperties(
            String supported,
            String defaultVersion
    ) {
    }
}
