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
@Document(collection = "friendships")
@CompoundIndex(
        name = "uk_friendship_pair",
        def = "{'userIdA': 1, 'userIdB': 1}",
        unique = true
)
public class Friendship {
    @Id
    private String id;

    @Indexed
    private String userIdA;

    @Indexed
    private String userIdB;

    @Indexed
    private List<String> userIds;

    private Instant createdAt;
}
