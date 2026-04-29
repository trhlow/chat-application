package com.chatrealtime.service;

import com.chatrealtime.domain.Message;
import com.chatrealtime.domain.MessageAttachment;
import com.chatrealtime.domain.Room;
import com.chatrealtime.exception.MessageNotFoundException;
import com.chatrealtime.exception.RoomNotFoundException;
import com.chatrealtime.repository.MessageAttachmentRepository;
import com.chatrealtime.repository.MessageRepository;
import com.chatrealtime.repository.RoomRepository;
import com.chatrealtime.security.AuthUserPrincipal;
import com.chatrealtime.storage.CloudinaryAttachmentSignedUrlService;
import com.chatrealtime.storage.MessageAttachmentProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

/**
 * Authorizes attachment access and returns either a local file or a signed Cloudinary redirect URI.
 */
@Service
@RequiredArgsConstructor
public class MessageAttachmentDownloadService {
    private static final String PROVIDER_LOCAL = "local";
    private static final String PROVIDER_CLOUDINARY = "cloudinary";

    private final MessageRepository messageRepository;
    private final RoomRepository roomRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final MessageAttachmentProperties attachmentProperties;
    private final CloudinaryAttachmentSignedUrlService cloudinaryAttachmentSignedUrlService;

    public AttachmentDeliveryResult resolveDelivery(
            AuthUserPrincipal principal,
            String messageId,
            String attachmentId,
            String variant
    ) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException("Message not found"));
        Room room = roomRepository.findById(message.getRoomId())
                .orElseThrow(() -> new RoomNotFoundException("Room not found"));
        if (room.getMemberIds() == null || !room.getMemberIds().contains(principal.getId())) {
            throw new AccessDeniedException("Forbidden");
        }

        MessageAttachment attachment = messageAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new MessageNotFoundException("Attachment not found"));
        if (!messageId.equals(attachment.getMessageId())) {
            throw new AccessDeniedException("Forbidden");
        }

        boolean thumbnail = variant != null && variant.equalsIgnoreCase("thumbnail");

        String provider = attachment.getStorageProvider() == null ? PROVIDER_LOCAL : attachment.getStorageProvider();
        MediaType mediaType = MediaType.parseMediaType(
                attachment.getMimeType() == null || attachment.getMimeType().isBlank()
                        ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                        : attachment.getMimeType()
        );
        String originalName = attachment.getOriginalName();

        if (PROVIDER_CLOUDINARY.equalsIgnoreCase(provider)) {
            var redirectUri = cloudinaryAttachmentSignedUrlService.buildSignedDeliveryUri(attachment, thumbnail);
            return new AttachmentDeliveryResult(
                    AttachmentDeliveryResult.Type.REDIRECT,
                    null,
                    redirectUri,
                    mediaType,
                    originalName
            );
        }

        if (!PROVIDER_LOCAL.equalsIgnoreCase(provider)) {
            throw new AccessDeniedException("Forbidden");
        }

        Path baseDir = Path.of(attachmentProperties.local().uploadDir()).toAbsolutePath().normalize();
        String relative = attachment.getStoragePublicId();
        if (relative == null || relative.isBlank() || relative.contains("..")) {
            throw new MessageNotFoundException("Attachment not found");
        }
        Path filePath = baseDir.resolve(relative).normalize();
        if (!filePath.startsWith(baseDir)) {
            throw new MessageNotFoundException("Attachment not found");
        }
        FileSystemResource body = new FileSystemResource(filePath);
        if (!body.exists() || !body.isReadable()) {
            throw new MessageNotFoundException("Attachment not found");
        }
        return new AttachmentDeliveryResult(
                AttachmentDeliveryResult.Type.LOCAL_RESOURCE,
                body,
                null,
                mediaType,
                originalName
        );
    }
}
