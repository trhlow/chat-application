package com.chatrealtime.service;

import com.chatrealtime.domain.User;
import com.chatrealtime.exception.UserNotFoundException;
import com.chatrealtime.repository.UserRepository;
import com.chatrealtime.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.nio.file.Path;

/**
 * Serves user avatars: Cloudinary via redirect; local files only after authorization.
 */
@Service
@RequiredArgsConstructor
public class UserAvatarDownloadService {
    private static final String PROVIDER_CLOUDINARY = "cloudinary";
    private static final String PROVIDER_LOCAL = "local";

    private final UserRepository userRepository;
    private final StorageProperties storageProperties;

    public AvatarDownloadResult resolve(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        if (user.getAvatar() == null || user.getAvatar().isBlank()) {
            throw new UserNotFoundException("User not found");
        }

        String provider = user.getAvatarProvider() == null ? PROVIDER_LOCAL : user.getAvatarProvider();
        if (PROVIDER_CLOUDINARY.equalsIgnoreCase(provider)) {
            return new AvatarDownloadResult.Redirect(URI.create(user.getAvatar()));
        }

        Path file = resolveLocalAvatarPath(user);
        if (file == null) {
            throw new UserNotFoundException("User not found");
        }
        FileSystemResource body = new FileSystemResource(file);
        if (!body.exists() || !body.isReadable()) {
            throw new UserNotFoundException("User not found");
        }
        MediaType mediaType = guessMediaType(user.getAvatar());
        return new AvatarDownloadResult.File(body, mediaType);
    }

    private Path resolveLocalAvatarPath(User user) {
        Path baseDir = Path.of(storageProperties.local().uploadDir()).toAbsolutePath().normalize();
        String publicId = user.getAvatarPublicId();
        if (publicId != null && !publicId.isBlank() && !publicId.contains("..")) {
            Path candidate = baseDir.resolve(publicId).normalize();
            if (candidate.startsWith(baseDir)) {
                return candidate;
            }
        }
        String avatar = user.getAvatar();
        if (avatar != null && avatar.contains("/uploads/avatars/")) {
            int idx = avatar.indexOf("/uploads/avatars/");
            String filename = avatar.substring(idx + "/uploads/avatars/".length());
            if (!filename.isBlank() && !filename.contains("..") && !filename.contains("/")) {
                Path candidate = baseDir.resolve(filename).normalize();
                if (candidate.startsWith(baseDir)) {
                    return candidate;
                }
            }
        }
        if (avatar != null && avatar.startsWith("/api/users/") && avatar.endsWith("/avatar")) {
            String publicIdFromApi = user.getAvatarPublicId();
            if (publicIdFromApi != null && !publicIdFromApi.isBlank() && !publicIdFromApi.contains("..")) {
                Path candidate = baseDir.resolve(publicIdFromApi).normalize();
                if (candidate.startsWith(baseDir)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static MediaType guessMediaType(String avatarUrl) {
        if (avatarUrl == null) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        String lower = avatarUrl.toLowerCase();
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.IMAGE_JPEG;
    }

    public sealed interface AvatarDownloadResult {
        record Redirect(URI location) implements AvatarDownloadResult {
        }

        record File(Resource body, MediaType mediaType) implements AvatarDownloadResult {
        }
    }
}
