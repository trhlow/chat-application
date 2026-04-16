package com.chatrealtime.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    private String displayName;

    @Column(length = 300)
    private String bio;

    @Column(length = 20)
    private String phone;

    private String themePreference;

    @Column(length = 512)
    private String avatar;

    private String avatarPublicId;
    private String avatarProvider;

    @Column(nullable = false)
    private boolean isOnline;

    @Column(nullable = false)
    private int tokenVersion;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastSeenAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}



