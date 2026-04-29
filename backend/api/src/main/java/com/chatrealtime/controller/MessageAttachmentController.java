package com.chatrealtime.controller;

import com.chatrealtime.security.AuthContextService;
import com.chatrealtime.service.AttachmentDeliveryResult;
import com.chatrealtime.service.MessageAttachmentDownloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageAttachmentController {
    private final AuthContextService authContextService;
    private final MessageAttachmentDownloadService messageAttachmentDownloadService;

    @GetMapping("/{messageId}/attachments/{attachmentId}/download")
    public ResponseEntity<?> downloadAttachment(
            @PathVariable String messageId,
            @PathVariable String attachmentId,
            @RequestParam(name = "variant", required = false) String variant
    ) {
        var principal = authContextService.requireCurrentUser();
        AttachmentDeliveryResult delivery =
                messageAttachmentDownloadService.resolveDelivery(principal, messageId, attachmentId, variant);

        if (delivery.type() == AttachmentDeliveryResult.Type.REDIRECT) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(delivery.redirectUri())
                    .build();
        }

        String mime = delivery.mediaType().toString();
        boolean inline = mime.startsWith("image/") || mime.startsWith("video/");
        String filename = delivery.originalName() == null || delivery.originalName().isBlank()
                ? "attachment"
                : delivery.originalName();
        ContentDisposition disposition = ContentDisposition.builder(inline ? "inline" : "attachment")
                .filename(filename, StandardCharsets.UTF_8)
                .build();

        Resource body = delivery.resource();
        return ResponseEntity.ok()
                .contentType(delivery.mediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
                .body(body);
    }
}
