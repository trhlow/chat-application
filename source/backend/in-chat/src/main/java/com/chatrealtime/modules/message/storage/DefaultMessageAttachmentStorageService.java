package com.chatrealtime.modules.message.storage;

import com.chatrealtime.exception.BadRequestException;
import com.chatrealtime.exception.FileStorageException;
import com.chatrealtime.modules.user.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DefaultMessageAttachmentStorageService implements MessageAttachmentStorageService {
    private static final String PROVIDER_CLOUDINARY = "cloudinary";
    private static final String PROVIDER_LOCAL = "local";
    private static final String TYPE_IMAGE = "image";
    private static final String TYPE_VIDEO = "video";
    private static final String TYPE_FILE = "file";

    private final MessageAttachmentProperties attachmentProperties;
    private final StorageProperties storageProperties;
    private final RestClient restClient = RestClient.create("https://api.cloudinary.com");

    @Override
    public StoredMessageAttachment store(String userId, MultipartFile file) {
        AttachmentMetadata metadata = scanMetadata(file);
        if (PROVIDER_CLOUDINARY.equalsIgnoreCase(storageProperties.provider())) {
            return uploadToCloudinary(userId, file, metadata);
        }
        return uploadToLocalStorage(userId, file, metadata);
    }

    private AttachmentMetadata scanMetadata(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("attachment file is required");
        }
        String mimeType = file.getContentType();
        if (mimeType == null || mimeType.isBlank()) {
            throw new BadRequestException("attachment mime type is required");
        }

        String normalizedMimeType = mimeType.toLowerCase(Locale.ROOT);
        String fileType = resolveFileType(normalizedMimeType);
        long maxSize = maxSizeFor(fileType);
        if (file.getSize() > maxSize) {
            throw new BadRequestException(fileType + " attachment exceeds the allowed size");
        }

        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null
                ? "attachment"
                : file.getOriginalFilename());
        if (originalName.contains("..")) {
            throw new BadRequestException("attachment filename is invalid");
        }

        return new AttachmentMetadata(fileType, normalizedMimeType, file.getSize(), originalName);
    }

    private StoredMessageAttachment uploadToLocalStorage(
            String userId,
            MultipartFile file,
            AttachmentMetadata metadata
    ) {
        try {
            Path uploadDir = Path.of(attachmentProperties.local().uploadDir(), metadata.fileType())
                    .toAbsolutePath()
                    .normalize();
            Files.createDirectories(uploadDir);

            String filename = userId + "-" + UUID.randomUUID() + extensionFor(metadata.mimeType(), metadata.originalName());
            Path target = uploadDir.resolve(filename).normalize();
            file.transferTo(target);

            String publicPath = "/uploads/message-attachments/" + metadata.fileType() + "/" + filename;
            String fileUrl = stripTrailingSlash(attachmentProperties.local().publicBaseUrl()) + publicPath;
            String thumbnailUrl = TYPE_IMAGE.equals(metadata.fileType()) ? fileUrl : null;

            return new StoredMessageAttachment(
                    fileUrl,
                    metadata.fileType(),
                    metadata.mimeType(),
                    metadata.fileSize(),
                    metadata.originalName(),
                    thumbnailUrl,
                    PROVIDER_LOCAL,
                    metadata.fileType() + "/" + filename
            );
        } catch (IOException exception) {
            throw new FileStorageException("Could not store message attachment");
        }
    }

    private StoredMessageAttachment uploadToCloudinary(
            String userId,
            MultipartFile file,
            AttachmentMetadata metadata
    ) {
        requireCloudinaryConfig();

        try {
            long timestamp = Instant.now().getEpochSecond();
            String publicId = "message-" + userId + "-" + UUID.randomUUID();
            String folder = attachmentProperties.cloudinary().folder() + "/" + metadata.fileType();
            String resourceType = cloudinaryResourceType(metadata.fileType());

            Map<String, String> signatureParams = new TreeMap<>();
            signatureParams.put("folder", folder);
            signatureParams.put("public_id", publicId);
            signatureParams.put("timestamp", String.valueOf(timestamp));

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new NamedByteArrayResource(file.getBytes(), metadata.originalName()));
            body.add("api_key", storageProperties.cloudinary().apiKey());
            body.add("folder", folder);
            body.add("public_id", publicId);
            body.add("timestamp", String.valueOf(timestamp));
            body.add("signature", sign(signatureParams, storageProperties.cloudinary().apiSecret()));

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/v1_1/{cloudName}/{resourceType}/upload", storageProperties.cloudinary().cloudName(), resourceType)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null || response.get("secure_url") == null || response.get("public_id") == null) {
                throw new FileStorageException("Cloudinary attachment upload failed");
            }

            String fileUrl = response.get("secure_url").toString();
            return new StoredMessageAttachment(
                    fileUrl,
                    metadata.fileType(),
                    metadata.mimeType(),
                    metadata.fileSize(),
                    metadata.originalName(),
                    TYPE_IMAGE.equals(metadata.fileType()) ? fileUrl : null,
                    PROVIDER_CLOUDINARY,
                    response.get("public_id").toString()
            );
        } catch (IOException exception) {
            throw new FileStorageException("Could not read message attachment");
        } catch (RuntimeException exception) {
            throw new FileStorageException("Cloudinary attachment upload failed");
        }
    }

    private String resolveFileType(String mimeType) {
        if (attachmentProperties.imageMimeTypes().contains(mimeType)) {
            return TYPE_IMAGE;
        }
        if (attachmentProperties.videoMimeTypes().contains(mimeType)) {
            return TYPE_VIDEO;
        }
        if (attachmentProperties.fileMimeTypes().contains(mimeType)) {
            return TYPE_FILE;
        }
        throw new BadRequestException("attachment mime type is not allowed");
    }

    private long maxSizeFor(String fileType) {
        return switch (fileType) {
            case TYPE_IMAGE -> attachmentProperties.imageMaxFileSizeBytes();
            case TYPE_VIDEO -> attachmentProperties.videoMaxFileSizeBytes();
            default -> attachmentProperties.fileMaxFileSizeBytes();
        };
    }

    private String cloudinaryResourceType(String fileType) {
        return switch (fileType) {
            case TYPE_IMAGE -> "image";
            case TYPE_VIDEO -> "video";
            default -> "raw";
        };
    }

    private String extensionFor(String mimeType, String originalName) {
        String extension = StringUtils.getFilenameExtension(originalName);
        if (extension != null && !extension.isBlank()) {
            return "." + extension.toLowerCase(Locale.ROOT);
        }
        return switch (mimeType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "video/mp4" -> ".mp4";
            case "video/webm" -> ".webm";
            case "application/pdf" -> ".pdf";
            case "application/zip" -> ".zip";
            default -> ".bin";
        };
    }

    private void requireCloudinaryConfig() {
        StorageProperties.Cloudinary cloudinary = storageProperties.cloudinary();
        if (isBlank(cloudinary.cloudName()) || isBlank(cloudinary.apiKey()) || isBlank(cloudinary.apiSecret())) {
            throw new FileStorageException("Cloudinary storage is not configured");
        }
    }

    private String sign(Map<String, String> params, String apiSecret) {
        String payload = params.entrySet()
                .stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + "&" + right)
                .orElse("") + apiSecret;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-1 is not available", exception);
        }
    }

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record AttachmentMetadata(
            String fileType,
            String mimeType,
            long fileSize,
            String originalName
    ) {
    }

    private static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename == null || filename.isBlank() ? "attachment" : filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
