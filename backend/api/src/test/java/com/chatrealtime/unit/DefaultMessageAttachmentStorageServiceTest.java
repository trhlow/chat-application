package com.chatrealtime.unit;

import com.chatrealtime.exception.BadRequestException;
import com.chatrealtime.storage.DefaultMessageAttachmentStorageService;
import com.chatrealtime.storage.FileMimeDetector;
import com.chatrealtime.storage.MessageAttachmentProperties;
import com.chatrealtime.storage.StorageProperties;
import com.chatrealtime.storage.StoredMessageAttachment;
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
class DefaultMessageAttachmentStorageServiceTest {
    @TempDir
    private Path uploadDir;

    @Mock
    private FileMimeDetector fileMimeDetector;

    @Test
    void store_WhenClientHeaderSpoofsImage_ShouldRejectDetectedMimeType() {
        DefaultMessageAttachmentStorageService storageService = newStorageService();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.png",
                "image/png",
                "not an image".getBytes()
        );

        when(fileMimeDetector.detect(file)).thenReturn("text/plain");

        assertThatThrownBy(() -> storageService.store("u1", file))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void store_ShouldUseDetectedMimeTypeForWhitelistAndMetadata() {
        DefaultMessageAttachmentStorageService storageService = newStorageService();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.txt",
                "text/plain",
                new byte[] {1, 2, 3}
        );

        when(fileMimeDetector.detect(file)).thenReturn("image/png");

        StoredMessageAttachment stored = storageService.store("u1", file);

        assertThat(stored.fileType()).isEqualTo("image");
        assertThat(stored.mimeType()).isEqualTo("image/png");
        assertThat(stored.storageProvider()).isEqualTo("local");
        assertThat(stored.storagePublicId()).startsWith("image/");
        assertThat(uploadDir.resolve(stored.storagePublicId())).exists();
    }

    private DefaultMessageAttachmentStorageService newStorageService() {
        return new DefaultMessageAttachmentStorageService(
                attachmentProperties(),
                storageProperties(),
                fileMimeDetector
        );
    }

    private MessageAttachmentProperties attachmentProperties() {
        return new MessageAttachmentProperties(
                1024,
                2048,
                4096,
                List.of("image/jpeg", "image/png", "image/webp"),
                List.of("video/mp4", "video/webm"),
                List.of("application/pdf", "application/zip"),
                new MessageAttachmentProperties.Local(uploadDir.toString(), "http://localhost:8080"),
                new MessageAttachmentProperties.Cloudinary("message-attachments"),
                300
        );
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
