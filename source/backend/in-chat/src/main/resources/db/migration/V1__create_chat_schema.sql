CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    display_name VARCHAR(255),
    bio VARCHAR(300),
    phone VARCHAR(20),
    theme_preference VARCHAR(255),
    avatar VARCHAR(512),
    avatar_public_id VARCHAR(255),
    avatar_provider VARCHAR(255),
    is_online BOOLEAN NOT NULL DEFAULT FALSE,
    token_version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    last_seen_at TIMESTAMP
);

CREATE TABLE rooms (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE room_members (
    room_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (room_id, user_id),
    CONSTRAINT fk_room_members_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE
);

CREATE TABLE messages (
    id VARCHAR(36) PRIMARY KEY,
    room_id VARCHAR(36) NOT NULL,
    sender_id VARCHAR(36) NOT NULL,
    content VARCHAR(4000) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL
);

CREATE INDEX idx_messages_room_timestamp ON messages(room_id, timestamp DESC);

CREATE TABLE message_delivered_users (
    message_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (message_id, user_id),
    CONSTRAINT fk_message_delivered_message FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE
);

CREATE TABLE message_read_users (
    message_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (message_id, user_id),
    CONSTRAINT fk_message_read_message FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE
);

CREATE TABLE notifications (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    related_id VARCHAR(36),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP
);

CREATE INDEX idx_notifications_user_created_at ON notifications(user_id, created_at DESC);

CREATE TABLE refresh_tokens (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP,
    expires_at TIMESTAMP,
    revoked_at TIMESTAMP,
    replaced_by_token_hash VARCHAR(255)
);

CREATE INDEX idx_refresh_tokens_user_active ON refresh_tokens(user_id, revoked_at);

CREATE TABLE friend_requests (
    id VARCHAR(36) PRIMARY KEY,
    requester_id VARCHAR(36) NOT NULL,
    receiver_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP,
    responded_at TIMESTAMP
);

CREATE INDEX idx_friend_requests_receiver_status_created ON friend_requests(receiver_id, status, created_at DESC);
CREATE INDEX idx_friend_requests_requester_status_created ON friend_requests(requester_id, status, created_at DESC);

CREATE TABLE friendships (
    id VARCHAR(36) PRIMARY KEY,
    user_ida VARCHAR(36) NOT NULL,
    user_idb VARCHAR(36) NOT NULL,
    created_at TIMESTAMP,
    CONSTRAINT uq_friendships_pair UNIQUE (user_ida, user_idb)
);

CREATE TABLE friendship_users (
    friendship_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (friendship_id, user_id),
    CONSTRAINT fk_friendship_users_friendship FOREIGN KEY (friendship_id) REFERENCES friendships(id) ON DELETE CASCADE
);

CREATE INDEX idx_friendship_users_user_id ON friendship_users(user_id);

CREATE TABLE message_attachments (
    id VARCHAR(36) PRIMARY KEY,
    message_id VARCHAR(36) NOT NULL,
    file_url VARCHAR(1000) NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    original_name VARCHAR(255),
    thumbnail_url VARCHAR(1000),
    storage_provider VARCHAR(255),
    storage_public_id VARCHAR(255),
    created_at TIMESTAMP,
    CONSTRAINT fk_message_attachments_message FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE
);

CREATE INDEX idx_message_attachments_message_id ON message_attachments(message_id);
