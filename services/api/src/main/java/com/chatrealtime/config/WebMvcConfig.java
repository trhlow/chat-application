package com.chatrealtime.config;

import com.chatrealtime.storage.MessageAttachmentProperties;
import com.chatrealtime.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    private final StorageProperties storageProperties;
    private final MessageAttachmentProperties messageAttachmentProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path avatarUploadDir = Path.of(storageProperties.local().uploadDir()).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations(withTrailingSlash(avatarUploadDir.toUri().toString()));

        Path attachmentUploadDir = Path.of(messageAttachmentProperties.local().uploadDir()).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/message-attachments/**")
                .addResourceLocations(withTrailingSlash(attachmentUploadDir.toUri().toString()));
    }

    private String withTrailingSlash(String value) {
        return value.endsWith("/") ? value : value + "/";
    }
}
