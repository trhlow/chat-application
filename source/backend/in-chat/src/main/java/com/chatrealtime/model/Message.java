package com.chatrealtime.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@Document(collection = "messages")
public class Message {
    @Id
    private String id;

    private String roomId;
    private String senderId;
    private String content;

    private LocalDateTime timestamp;
    private String status; // e.g. sent, delivered, read
}

