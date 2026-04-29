package com.chatrealtime.storage;

import com.cloudinary.AuthToken;
import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.Url;
import com.cloudinary.utils.ObjectUtils;
import com.chatrealtime.domain.MessageAttachment;
import com.chatrealtime.exception.FileStorageException;
import com.chatrealtime.exception.MessageNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds time-limited signed Cloudinary delivery URLs (302 redirect); never streams bytes through the API.
 */
@Service
@RequiredArgsConstructor
public class CloudinaryAttachmentSignedUrlService {
    private static final Pattern VERSION_IN_PATH = Pattern.compile("/v(\\d+)/");

    private final StorageProperties storageProperties;
    private final MessageAttachmentProperties messageAttachmentProperties;

    public URI buildSignedDeliveryUri(MessageAttachment attachment, boolean thumbnailVariant) {
        StorageProperties.Cloudinary cfg = storageProperties.cloudinary();
        if (isBlank(cfg.cloudName()) || isBlank(cfg.apiSecret()) || isBlank(cfg.apiKey())) {
            throw new FileStorageException("Cloudinary storage is not configured");
        }
        validateCloudinaryFileUrl(attachment.getFileUrl(), cfg.cloudName());

        String publicId = attachment.getStoragePublicId();
        if (publicId == null || publicId.isBlank()) {
            throw new MessageNotFoundException("Attachment not found");
        }

        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cfg.cloudName(),
                "api_key", cfg.apiKey(),
                "api_secret", cfg.apiSecret(),
                "secure", true
        ));

        String resourceType = cloudinaryResourceType(attachment.getFileType());
        Url url = cloudinary.url()
                .type("upload")
                .resourceType(resourceType)
                .publicId(publicId)
                .secure(true)
                .signed(true);

        Long version = parseVersionFromUrl(attachment.getFileUrl());
        if (version != null) {
            url.version(version);
        }

        if (thumbnailVariant && "image".equalsIgnoreCase(attachment.getFileType())) {
            url.transformation(new Transformation().crop("limit").width(400).height(400).quality("auto"));
        }

        int ttl = messageAttachmentProperties.signedUrlTtlSeconds();
        if (ttl > 0) {
            url.authToken(new AuthToken().duration(ttl));
        }

        try {
            return URI.create(url.generate());
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not build Cloudinary delivery URL");
        }
    }

    private static void validateCloudinaryFileUrl(String fileUrl, String expectedCloudName) {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new MessageNotFoundException("Attachment not found");
        }
        URI uri = URI.create(fileUrl);
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new AccessDeniedException("Invalid attachment URL");
        }
        if (!"res.cloudinary.com".equalsIgnoreCase(uri.getHost())) {
            throw new AccessDeniedException("Invalid attachment host");
        }
        String path = uri.getPath();
        if (path == null || !path.contains("/" + expectedCloudName + "/")) {
            throw new AccessDeniedException("Invalid Cloudinary resource path");
        }
    }

    private static Long parseVersionFromUrl(String fileUrl) {
        Matcher matcher = VERSION_IN_PATH.matcher(URI.create(fileUrl).getPath());
        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String cloudinaryResourceType(String fileType) {
        if (fileType == null) {
            return "raw";
        }
        return switch (fileType.toLowerCase()) {
            case "image" -> "image";
            case "video" -> "video";
            default -> "raw";
        };
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
