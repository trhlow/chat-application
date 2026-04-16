package com.chatrealtime.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@Configuration
public class VirtualThreadConfig {
    @Bean(name = "applicationTaskExecutor")
    @ConditionalOnProperty(prefix = "spring.threads.virtual", name = "enabled", havingValue = "true")
    public AsyncTaskExecutor applicationTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("app-vt-");
        executor.setVirtualThreads(true);
        return executor;
    }
}
