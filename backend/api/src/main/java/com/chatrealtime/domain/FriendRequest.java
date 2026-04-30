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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "friend_requests")
@CompoundIndex(
        name = "uk_friend_request_pair_status",
        def = "{'userIdA': 1, 'userIdB': 1, 'status': 1}",
        unique = true
)
public class FriendRequest {
    @Id
    private String id;

    @Indexed
    private String requesterId;

    @Indexed
    private String receiverId;

    @Indexed
    private String userIdA;

    @Indexed
    private String userIdB;

    @Indexed
    private FriendRequestStatus status;

    private Instant createdAt;
    private Instant respondedAt;
}
