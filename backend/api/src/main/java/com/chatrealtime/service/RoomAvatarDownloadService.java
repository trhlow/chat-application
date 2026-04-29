package com.chatrealtime.service;

import com.chatrealtime.domain.Room;
import com.chatrealtime.exception.RoomNotFoundException;
import com.chatrealtime.repository.RoomRepository;
import com.chatrealtime.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.nio.file.Path;

/**
 * Serves group room avatars (local disk or Cloudinary redirect) for room members only.
 */
@Service
@RequiredArgsConstructor
public class RoomAvatarDownloadService {
    private static final String PROVIDER_CLOUDINARY = "cloudinary";
    private static final String PROVIDER_LOCAL = "local";

    private final RoomRepository roomRepository;
    private final StorageProperties storageProperties;

    public RoomAvatarDownloadResult resolve(String roomId, String actorUserId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found"));
        if (room.getMemberIds() == null || !room.getMemberIds().contains(actorUserId)) {
            throw new AccessDeniedException("Forbidden");
        }
        if (room.getAvatar() == null || room.getAvatar().isBlank()) {
            throw new RoomNotFoundException("Room not found");
        }

        String provider = room.getAvatarProvider() == null ? PROVIDER_LOCAL : room.getAvatarProvider();
        if (PROVIDER_CLOUDINARY.equalsIgnoreCase(provider)) {
            return new RoomAvatarDownloadResult.Redirect(URI.create(room.getAvatar()));
        }

        Path file = resolveLocalAvatarPath(room);
        if (file == null) {
            throw new RoomNotFoundException("Room not found");
        }
        FileSystemResource body = new FileSystemResource(file);
        if (!body.exists() || !body.isReadable()) {
            throw new RoomNotFoundException("Room not found");
        }
        MediaType mediaType = guessMediaType(room.getAvatar());
        return new RoomAvatarDownloadResult.File(body, mediaType);
    }

    private Path resolveLocalAvatarPath(Room room) {
        Path baseDir = Path.of(storageProperties.local().uploadDir()).toAbsolutePath().normalize();
        String publicId = room.getAvatarPublicId();
        if (publicId != null && !publicId.isBlank() && !publicId.contains("..")) {
            Path candidate = baseDir.resolve(publicId).normalize();
            if (candidate.startsWith(baseDir)) {
                return candidate;
            }
        }
        String avatar = room.getAvatar();
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
        if (avatar != null && avatar.contains("/api/rooms/") && avatar.endsWith("/avatar")) {
            String publicIdFromApi = room.getAvatarPublicId();
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

    public sealed interface RoomAvatarDownloadResult {
        record Redirect(URI location) implements RoomAvatarDownloadResult {
        }

        record File(Resource body, MediaType mediaType) implements RoomAvatarDownloadResult {
        }
    }
}
