package com.chatrealtime.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "rooms")
public class Room {
    @Id
    private String id;

    private String name;
    private String type;
    private String avatar;
    private String avatarPublicId;
    private String avatarProvider;
    @Indexed
    private List<String> memberIds;
    private List<String> admins;

    private String createdBy;
    private String ownerId;
    private Instant lastMessageAt;
    private String lastMessagePreview;
    private Instant createdAt;
    private Instant updatedAt;
}



