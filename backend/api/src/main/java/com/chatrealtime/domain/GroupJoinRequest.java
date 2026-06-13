package com.chatrealtime.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
        @CompoundIndex(name = "idx_group_join_request_room_status", def = "{'roomId': 1, 'status': 1, 'createdAt': -1}"),
        @CompoundIndex(
                name = "uk_group_join_request_pending_target",
                def = "{'roomId': 1, 'targetUserId': 1, 'status': 1}",
                unique = true,
                partialFilter = "{'status': 'PENDING'}"
        )
})
@Document(collection = "group_join_requests")
public class GroupJoinRequest {
    @Id
    private String id;

    private String roomId;
    private String requesterId;
    private String targetUserId;
    private GroupJoinRequestStatus status;
    private Instant createdAt;
    private Instant respondedAt;
    private String respondedBy;
}
