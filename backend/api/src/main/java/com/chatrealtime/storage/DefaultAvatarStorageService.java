package com.chatrealtime.storage;

import com.chatrealtime.exception.BadRequestException;
import com.chatrealtime.exception.FileStorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
public class DefaultAvatarStorageService implements AvatarStorageService {
    private static final String PROVIDER_CLOUDINARY = "cloudinary";
    private static final String PROVIDER_LOCAL = "local";

    private final StorageProperties storageProperties;
    private final FileMimeDetector fileMimeDetector;
    private final RestClient restClient = RestClient.create("https://api.cloudinary.com");

    @Override
    public AvatarUploadResult uploadAvatar(String userId, MultipartFile file) {
        validateFileRequired(file);
        String detectedMimeType = fileMimeDetector.detect(file);
        validateFile(file, detectedMimeType);
        if (PROVIDER_CLOUDINARY.equalsIgnoreCase(storageProperties.provider())) {
            return uploadToCloudinary(userId, file);
        }
        return uploadToLocalStorage(userId, file, detectedMimeType);
    }

    @Override
    public void deleteAvatar(String provider, String publicId) {
        if (provider == null || publicId == null || publicId.isBlank()) {
            return;
        }

        if (PROVIDER_CLOUDINARY.equalsIgnoreCase(provider)) {
            deleteFromCloudinary(publicId);
            return;
        }

        if (PROVIDER_LOCAL.equalsIgnoreCase(provider)) {
            deleteFromLocalStorage(publicId);
        }
    }

    private void validateFileRequired(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("avatar file is required");
        }
    }

    private void validateFile(MultipartFile file, String detectedMimeType) {
        if (file.getSize() > storageProperties.maxFileSizeBytes()) {
            throw new BadRequestException("avatar file size exceeds the allowed limit");
        }
        if (detectedMimeType == null || detectedMimeType.isBlank() || storageProperties.allowedContentTypes().stream()
                .noneMatch(contentType -> contentType.equalsIgnoreCase(detectedMimeType))) {
            throw new BadRequestException("avatar file type must be image/jpeg, image/png, or image/webp");
        }
    }

    private AvatarUploadResult uploadToLocalStorage(String userId, MultipartFile file, String detectedMimeType) {
        try {
            Path uploadDir = Path.of(storageProperties.local().uploadDir()).toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);

            String extension = extensionFromContentType(detectedMimeType);
            String filename = userId + "-" + UUID.randomUUID() + extension;
            Path target = uploadDir.resolve(filename).normalize();
            file.transferTo(target);

            String url;
            if (userId.startsWith("room-")) {
                String roomId = userId.substring("room-".length());
                url = "/api/rooms/" + roomId + "/avatar";
            } else {
                url = "/api/users/" + userId + "/avatar";
            }
            return new AvatarUploadResult(url, filename, PROVIDER_LOCAL);
        } catch (IOException exception) {
            throw new FileStorageException("Could not store avatar file");
        }
    }

    private AvatarUploadResult uploadToCloudinary(String userId, MultipartFile file) {
        StorageProperties.Cloudinary cloudinary = storageProperties.cloudinary();
        requireCloudinaryConfig(cloudinary);

        try {
            long timestamp = Instant.now().getEpochSecond();
            Map<String, String> signatureParams = new TreeMap<>();
            signatureParams.put("folder", cloudinary.folder());
            signatureParams.put("public_id", "user-" + userId + "-" + UUID.randomUUID());
            signatureParams.put("timestamp", String.valueOf(timestamp));

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new NamedByteArrayResource(file.getBytes(), file.getOriginalFilename()));
            body.add("api_key", cloudinary.apiKey());
            body.add("folder", cloudinary.folder());
            body.add("public_id", signatureParams.get("public_id"));
            body.add("timestamp", String.valueOf(timestamp));
            body.add("signature", sign(signatureParams, cloudinary.apiSecret()));

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/v1_1/{cloudName}/image/upload", cloudinary.cloudName())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null || response.get("secure_url") == null || response.get("public_id") == null) {
                throw new FileStorageException("Cloudinary upload failed");
            }

            return new AvatarUploadResult(
                    response.get("secure_url").toString(),
                    response.get("public_id").toString(),
                    PROVIDER_CLOUDINARY
            );
        } catch (IOException exception) {
            throw new FileStorageException("Could not read avatar file");
        } catch (RuntimeException exception) {
            throw new FileStorageException("Cloudinary upload failed");
        }
    }

    private void deleteFromLocalStorage(String publicId) {
        try {
            Path uploadDir = Path.of(storageProperties.local().uploadDir()).toAbsolutePath().normalize();
            Path target = uploadDir.resolve(publicId).normalize();
            if (target.startsWith(uploadDir)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException ignored) {
        }
    }

    private void deleteFromCloudinary(String publicId) {
        StorageProperties.Cloudinary cloudinary = storageProperties.cloudinary();
        requireCloudinaryConfig(cloudinary);

        long timestamp = Instant.now().getEpochSecond();
        Map<String, String> signatureParams = new TreeMap<>();
        signatureParams.put("public_id", publicId);
        signatureParams.put("timestamp", String.valueOf(timestamp));

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("api_key", cloudinary.apiKey());
        body.add("public_id", publicId);
        body.add("timestamp", String.valueOf(timestamp));
        body.add("signature", sign(signatureParams, cloudinary.apiSecret()));

        restClient.post()
                .uri("/v1_1/{cloudName}/image/destroy", cloudinary.cloudName())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private void requireCloudinaryConfig(StorageProperties.Cloudinary cloudinary) {
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

    private String extensionFromContentType(String contentType) {
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new BadRequestException("avatar file type must be image/jpeg, image/png, or image/webp");
        };
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename == null || filename.isBlank() ? "avatar" : filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
