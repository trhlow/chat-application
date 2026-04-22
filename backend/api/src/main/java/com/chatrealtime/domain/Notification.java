package com.chatrealtime.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
        @CompoundIndex(name = "idx_notification_user_created", def = "{'userId': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "idx_notification_user_read_created", def = "{'userId': 1, 'read': 1, 'createdAt': -1}")
})
@Document(collection = "notifications")
public class Notification {
    @Id
    private String id;

    @Indexed
    private String userId;

    private String type;
    private String title;
    private String message;
    private String relatedId;
    private boolean read;
    private Instant createdAt;
}



