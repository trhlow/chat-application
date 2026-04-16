package com.chatrealtime.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "message_attachments")
public class MessageAttachment {
    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 36)
    private String messageId;

    @Column(nullable = false, length = 1000)
    private String fileUrl;

    @Column(nullable = false, length = 20)
    private String fileType;

    @Column(nullable = false, length = 100)
    private String mimeType;

    @Column(nullable = false)
    private long fileSize;

    private String originalName;

    @Column(length = 1000)
    private String thumbnailUrl;

    private String storageProvider;
    private String storagePublicId;
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
