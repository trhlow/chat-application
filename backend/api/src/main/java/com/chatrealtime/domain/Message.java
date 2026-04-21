package com.chatrealtime.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
        @CompoundIndex(name = "idx_message_room_timestamp", def = "{'roomId': 1, 'timestamp': -1}"),
        @CompoundIndex(name = "idx_message_room_delivered_state", def = "{'roomId': 1, 'senderId': 1, 'deliveredToUserIds': 1, 'timestamp': -1}"),
        @CompoundIndex(name = "idx_message_room_seen_state", def = "{'roomId': 1, 'senderId': 1, 'readByUserIds': 1, 'timestamp': -1}")
})
@Document(collection = "messages")
public class Message {
    @Id
    private String id;

    private String roomId;
    private String senderId;
    private String content;
    private LocalDateTime timestamp;
    private String status;
    private Set<String> deliveredToUserIds;
    private Set<String> readByUserIds;
}



