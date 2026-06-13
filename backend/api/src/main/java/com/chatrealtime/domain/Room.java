package com.chatrealtime.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(
        name = "uk_direct_room_key",
        def = "{'type': 1, 'directKey': 1}",
        unique = true,
        partialFilter = "{'type': 'direct', 'directKey': {'$type': 'string'}}"
)
@Document(collection = "rooms")
public class Room {
    @Id
    private String id;

    private String name;
    private String type;
    private String avatar;
    private String avatarPublicId;
    private String avatarProvider;
    private String directKey;
    @Indexed
    private List<String> memberIds;
    private List<String> admins;
    private GroupSettings settings;

    private String createdBy;
    private String ownerId;
    private Instant lastMessageAt;
    private String lastMessagePreview;
    private Instant createdAt;
    private Instant updatedAt;
}



