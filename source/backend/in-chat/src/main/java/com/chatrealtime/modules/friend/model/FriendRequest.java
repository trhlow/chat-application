package com.chatrealtime.modules.friend.model;

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
@Document(collection = "friend_requests")
public class FriendRequest {
    @Id
    private String id;

    @Indexed
    private String requesterId;

    @Indexed
    private String receiverId;

    @Indexed
    private FriendRequestStatus status;

    private Instant createdAt;
    private Instant respondedAt;
}
