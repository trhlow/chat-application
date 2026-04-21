package com.chatrealtime.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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



