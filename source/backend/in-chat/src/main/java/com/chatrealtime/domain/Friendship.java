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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "friendships")
public class Friendship {
    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 36)
    private String userIdA;

    @Column(nullable = false, length = 36)
    private String userIdB;

    @ElementCollection
    @CollectionTable(name = "friendship_users", joinColumns = @JoinColumn(name = "friendship_id"))
    @Column(name = "user_id", length = 36)
    private List<String> userIds;

    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
