package com.chatrealtime.domain;

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
@Document(collection = "users")
public class User {
    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    private String password;

    @Indexed(unique = true)
    private String email;

    private String displayName;
    private String bio;
    private String phone;
    private String themePreference;
    private String avatar;
    private String avatarPublicId;
    private String avatarProvider;
    private boolean isOnline;
    private int tokenVersion;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastSeenAt;
}



