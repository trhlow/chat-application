package com.chatrealtime.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "messages")
public class Message {
    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 36)
    private String roomId;

    @Column(nullable = false, length = 36)
    private String senderId;

    @Column(nullable = false, length = 4000)
    private String content;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false, length = 20)
    private String status;

    @ElementCollection
    @CollectionTable(name = "message_delivered_users", joinColumns = @JoinColumn(name = "message_id"))
    @Column(name = "user_id", length = 36)
    private Set<String> deliveredToUserIds;

    @ElementCollection
    @CollectionTable(name = "message_read_users", joinColumns = @JoinColumn(name = "message_id"))
    @Column(name = "user_id", length = 36)
    private Set<String> readByUserIds;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}



