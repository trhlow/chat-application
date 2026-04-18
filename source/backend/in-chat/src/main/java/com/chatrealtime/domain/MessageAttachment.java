package com.chatrealtime.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "message_attachments")
public class MessageAttachment {
    @Id
    private String id;

    @Indexed
    private String messageId;

    private String fileUrl;
    private String fileType;
    private String mimeType;
    private long fileSize;
    private String originalName;
    private String thumbnailUrl;
    private String storageProvider;
    private String storagePublicId;
    private Instant createdAt;
}
