package com.chatrealtime.unit;

import com.chatrealtime.exception.BadRequestException;
import com.chatrealtime.storage.AvatarUploadResult;
import com.chatrealtime.storage.DefaultAvatarStorageService;
import com.chatrealtime.storage.FileMimeDetector;
import com.chatrealtime.storage.StorageProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultAvatarStorageServiceTest {
    @TempDir
    private Path uploadDir;

    @Mock
    private FileMimeDetector fileMimeDetector;

    @Test
    void uploadAvatar_WhenClientHeaderSpoofsImage_ShouldRejectDetectedMimeType() {
        DefaultAvatarStorageService storageService = new DefaultAvatarStorageService(storageProperties(), fileMimeDetector);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "not an image".getBytes()
        );

        when(fileMimeDetector.detect(file)).thenReturn("text/plain");

        assertThatThrownBy(() -> storageService.uploadAvatar("u1", file))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void uploadAvatar_ShouldUseDetectedMimeTypeForLocalExtensionAndEndpoint() {
        DefaultAvatarStorageService storageService = new DefaultAvatarStorageService(storageProperties(), fileMimeDetector);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.txt",
                "text/plain",
                new byte[] {1, 2, 3}
        );

        when(fileMimeDetector.detect(file)).thenReturn("image/png");

        AvatarUploadResult result = storageService.uploadAvatar("u1", file);

        assertThat(result.url()).isEqualTo("/api/users/u1/avatar");
        assertThat(result.provider()).isEqualTo("local");
        assertThat(result.publicId()).endsWith(".png");
        assertThat(uploadDir.resolve(result.publicId())).exists();
    }

    private StorageProperties storageProperties() {
        return new StorageProperties(
                "local",
                1024,
                List.of("image/jpeg", "image/png", "image/webp"),
                new StorageProperties.Local(uploadDir.toString(), "http://localhost:8080"),
                new StorageProperties.Cloudinary("", "", "", "")
        );
    }
}
