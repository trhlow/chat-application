package com.chatrealtime.service;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

import java.net.URI;

/**
 * Result of resolving an attachment for download: stream from disk or redirect to CDN.
 */
public record AttachmentDeliveryResult(
        Type type,
        Resource resource,
        URI redirectUri,
        MediaType mediaType,
        String originalName
) {
    public enum Type {
        LOCAL_RESOURCE,
        REDIRECT
    }
}
