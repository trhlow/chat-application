package com.chatrealtime.integration;

import com.chatrealtime.domain.Friendship;
import com.chatrealtime.domain.Message;
import com.chatrealtime.domain.MessageAttachment;
import com.chatrealtime.domain.Room;
import com.chatrealtime.domain.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.chatrealtime.repository.FriendshipRepository;
import com.chatrealtime.repository.MessageAttachmentRepository;
import com.chatrealtime.repository.MessageRepository;
import com.chatrealtime.repository.RoomRepository;
import com.chatrealtime.repository.UserRepository;
import com.chatrealtime.security.AuthUserPrincipal;
import com.chatrealtime.security.JwtTokenService;
import com.chatrealtime.storage.MessageAttachmentProperties;
import com.chatrealtime.util.UserIdPair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PrivacySecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private MessageAttachmentRepository messageAttachmentRepository;
    @Autowired
    private FriendshipRepository friendshipRepository;
    @Autowired
    private JwtTokenService jwtTokenService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private MessageAttachmentProperties messageAttachmentProperties;

    private String userA;
    private String userB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void seedUsers() {
        userRepository.deleteAll();
        roomRepository.deleteAll();
        messageRepository.deleteAll();
        messageAttachmentRepository.deleteAll();
        friendshipRepository.deleteAll();

        userA = "it-user-a-" + UUID.randomUUID();
        userB = "it-user-b-" + UUID.randomUUID();

        userRepository.save(User.builder()
                .id(userA)
                .username("alice_" + userA.substring(0, 8))
                .email("a@test.local")
                .password(passwordEncoder.encode("Password123!"))
                .displayName("Alice IT")
                .tokenVersion(0)
                .isOnline(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        userRepository.save(User.builder()
                .id(userB)
                .username("bob_" + userB.substring(0, 8))
                .email("b@test.local")
                .password(passwordEncoder.encode("Password123!"))
                .displayName("Bob IT")
                .tokenVersion(0)
                .isOnline(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        tokenA = jwtTokenService.generateToken(new AuthUserPrincipal(userA, "alice", "x", 0));
        tokenB = jwtTokenService.generateToken(new AuthUserPrincipal(userB, "bob", "x", 0));
    }

    @Test
    void attachmentDownload_nonMember_returns403() throws Exception {
        String roomId = "it-room-" + UUID.randomUUID();
        roomRepository.save(Room.builder()
                .id(roomId)
                .type("direct")
                .memberIds(List.of(userA))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        String messageId = "it-msg-" + UUID.randomUUID();
        messageRepository.save(Message.builder()
                .id(messageId)
                .roomId(roomId)
                .senderId(userA)
                .content("hi")
                .timestamp(LocalDateTime.now())
                .status("sent")
                .deliveredToUserIds(Set.of(userA))
                .readByUserIds(Set.of(userA))
                .build());

        Path uploadRoot = Path.of(messageAttachmentProperties.local().uploadDir()).toAbsolutePath().normalize();
        Files.createDirectories(uploadRoot.resolve("image"));
        String relative = "image/" + userA + "-it.bin";
        Path filePath = uploadRoot.resolve(relative).normalize();
        Files.writeString(filePath, "local-bytes", StandardCharsets.UTF_8);

        String attId = "it-att-" + UUID.randomUUID();
        messageAttachmentRepository.save(MessageAttachment.builder()
                .id(attId)
                .messageId(messageId)
                .fileUrl("http://local/ignore")
                .fileType("image")
                .mimeType("image/png")
                .fileSize(4)
                .originalName("x.png")
                .thumbnailUrl(null)
                .storageProvider("local")
                .storagePublicId(relative)
                .createdAt(Instant.now())
                .build());

        mockMvc.perform(get("/api/messages/{mid}/attachments/{aid}/download", messageId, attId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    @Test
    void attachmentDownload_messageIdMismatch_returns403() throws Exception {
        String roomId = "it-room-" + UUID.randomUUID();
        roomRepository.save(Room.builder()
                .id(roomId)
                .type("group")
                .memberIds(List.of(userA, userB))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        String messageId = "it-msg-" + UUID.randomUUID();
        String otherMessageId = "it-msg-other-" + UUID.randomUUID();
        messageRepository.save(Message.builder()
                .id(messageId)
                .roomId(roomId)
                .senderId(userA)
                .content("hi")
                .timestamp(LocalDateTime.now())
                .status("sent")
                .deliveredToUserIds(Set.of(userA, userB))
                .readByUserIds(Set.of(userA, userB))
                .build());
        messageRepository.save(Message.builder()
                .id(otherMessageId)
                .roomId(roomId)
                .senderId(userA)
                .content("other")
                .timestamp(LocalDateTime.now())
                .status("sent")
                .deliveredToUserIds(Set.of(userA, userB))
                .readByUserIds(Set.of(userA, userB))
                .build());

        Path uploadRoot = Path.of(messageAttachmentProperties.local().uploadDir()).toAbsolutePath().normalize();
        Files.createDirectories(uploadRoot.resolve("image"));
        String relative = "image/" + userA + "-it2.bin";
        Files.writeString(uploadRoot.resolve(relative).normalize(), "x", StandardCharsets.UTF_8);

        String attId = "it-att-" + UUID.randomUUID();
        messageAttachmentRepository.save(MessageAttachment.builder()
                .id(attId)
                .messageId(messageId)
                .fileUrl("http://local/ignore")
                .fileType("image")
                .mimeType("image/png")
                .fileSize(1)
                .originalName("x.png")
                .thumbnailUrl(null)
                .storageProvider("local")
                .storagePublicId(relative)
                .createdAt(Instant.now())
                .build());

        mockMvc.perform(get("/api/messages/{mid}/attachments/{aid}/download", otherMessageId, attId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    @Test
    void messageResponse_doesNotExposeRawAttachmentUrl() throws Exception {
        String roomId = "it-room-attachment-response-" + UUID.randomUUID();
        roomRepository.save(Room.builder()
                .id(roomId)
                .type("group")
                .memberIds(List.of(userA, userB))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        String messageId = "it-msg-attachment-response-" + UUID.randomUUID();
        messageRepository.save(Message.builder()
                .id(messageId)
                .roomId(roomId)
                .senderId(userA)
                .content("with attachment")
                .timestamp(LocalDateTime.now())
                .status("sent")
                .deliveredToUserIds(Set.of(userA, userB))
                .readByUserIds(Set.of(userA))
                .build());

        String attachmentId = "it-att-response-" + UUID.randomUUID();
        messageAttachmentRepository.save(MessageAttachment.builder()
                .id(attachmentId)
                .messageId(messageId)
                .fileUrl("https://res.cloudinary.com/demo/image/upload/private-attachment.png")
                .fileType("image")
                .mimeType("image/png")
                .fileSize(123)
                .originalName("private.png")
                .thumbnailUrl("http://localhost:8080/uploads/message-attachments/image/private.png")
                .storageProvider("cloudinary")
                .storagePublicId("private-attachment")
                .createdAt(Instant.now())
                .build());

        String body = mockMvc.perform(get("/api/messages").param("roomId", roomId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode attachment = objectMapper.readTree(body)
                .get("items")
                .get(0)
                .get("attachments")
                .get(0);

        assertThat(attachment.has("fileUrl")).isFalse();
        assertThat(attachment.has("thumbnailUrl")).isFalse();
        assertThat(attachment.get("downloadEndpoint").asText())
                .isEqualTo("/api/messages/" + messageId + "/attachments/" + attachmentId + "/download");
        assertThat(body).doesNotContain("res.cloudinary.com");
        assertThat(body).doesNotContain("/uploads/");
    }

    @Test
    void avatar_stranger_returns403() throws Exception {
        saveUserWithCloudinaryAvatar(userA);

        mockMvc.perform(get("/api/users/{id}/avatar", userA)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    @Test
    void avatar_friend_canRedirect() throws Exception {
        saveUserWithCloudinaryAvatar(userA);

        UserIdPair.Ordered pair = UserIdPair.order(userA, userB);
        friendshipRepository.save(Friendship.builder()
                .id("it-fr-" + UUID.randomUUID())
                .userIdA(pair.userIdA())
                .userIdB(pair.userIdB())
                .userIds(List.of(pair.userIdA(), pair.userIdB()))
                .createdAt(Instant.now())
                .build());

        mockMvc.perform(get("/api/users/{id}/avatar", userA)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isFound());
    }

    @Test
    void avatar_sharedRoom_canRedirect() throws Exception {
        saveUserWithCloudinaryAvatar(userA);

        String roomId = "it-room-shared-" + UUID.randomUUID();
        roomRepository.save(Room.builder()
                .id(roomId)
                .type("group")
                .memberIds(List.of(userA, userB))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        mockMvc.perform(get("/api/users/{id}/avatar", userA)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isFound());
    }

    @Test
    void userSearch_responseHasNoEmailField() throws Exception {
        String body = mockMvc.perform(get("/api/users").param("query", "alice")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain("\"email\"");
    }

    @Test
    void userProfileResponses_doNotExposeRawAvatarUrl() throws Exception {
        saveUserWithCloudinaryAvatar(userA);

        String meBody = mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode me = objectMapper.readTree(meBody);
        assertThat(me.has("avatarProvider")).isFalse();
        assertThat(me.get("avatarEndpoint").asText()).isEqualTo("/api/users/" + userA + "/avatar");
        assertThat(me.get("avatar").asText()).isEqualTo("/api/users/" + userA + "/avatar");
        assertThat(meBody).doesNotContain("res.cloudinary.com");
        assertThat(meBody).doesNotContain("/uploads/");

        String publicBody = mockMvc.perform(get("/api/users/{id}", userA)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode publicProfile = objectMapper.readTree(publicBody);
        assertThat(publicProfile.has("avatarProvider")).isFalse();
        assertThat(publicProfile.get("avatarEndpoint").asText()).isEqualTo("/api/users/" + userA + "/avatar");
        assertThat(publicProfile.get("avatar").asText()).isEqualTo("/api/users/" + userA + "/avatar");
        assertThat(publicBody).doesNotContain("res.cloudinary.com");
        assertThat(publicBody).doesNotContain("/uploads/");
    }

    @Test
    void roomResponse_doesNotExposeRawAvatarUrl() throws Exception {
        String roomId = "it-room-avatar-" + UUID.randomUUID();
        roomRepository.save(Room.builder()
                .id(roomId)
                .name("Private room")
                .type("group")
                .avatar("https://res.cloudinary.com/demo/image/upload/room-avatar")
                .avatarProvider("cloudinary")
                .memberIds(List.of(userA, userB))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        String body = mockMvc.perform(get("/api/rooms/{id}", roomId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode room = objectMapper.readTree(body);
        assertThat(room.has("avatarProvider")).isFalse();
        assertThat(room.get("avatarEndpoint").asText()).isEqualTo("/api/rooms/" + roomId + "/avatar");
        assertThat(room.get("avatar").asText()).isEqualTo("/api/rooms/" + roomId + "/avatar");
        assertThat(body).doesNotContain("res.cloudinary.com");
        assertThat(body).doesNotContain("/uploads/");
    }

    @Test
    void getFriends_responseDoesNotExposeEmailOrPrivateFields() throws Exception {
        UserIdPair.Ordered pair = UserIdPair.order(userA, userB);
        friendshipRepository.save(Friendship.builder()
                .id("it-fr-" + UUID.randomUUID())
                .userIdA(pair.userIdA())
                .userIdB(pair.userIdB())
                .userIds(List.of(pair.userIdA(), pair.userIdB()))
                .createdAt(Instant.now())
                .build());

        String body = mockMvc.perform(get("/api/friends")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(body);

        assertThat(root).hasSize(1);
        JsonNode friend = root.get(0).get("friend");
        assertThat(friend.get("id").asText()).isEqualTo(userB);
        assertThat(friend.has("username")).isTrue();
        assertThat(friend.has("displayName")).isTrue();
        assertThat(friend.has("avatarEndpoint")).isTrue();
        assertThat(friend.has("email")).isFalse();
        assertThat(friend.has("phone")).isFalse();
        assertThat(friend.has("bio")).isFalse();
        assertThat(friend.has("themePreference")).isFalse();
        assertThat(friend.has("lastSeenAt")).isFalse();
    }

    private void saveUserWithCloudinaryAvatar(String userId) {
        User u = userRepository.findById(userId).orElseThrow();
        userRepository.save(User.builder()
                .id(u.getId())
                .username(u.getUsername())
                .password(u.getPassword())
                .email(u.getEmail())
                .displayName(u.getDisplayName())
                .bio(u.getBio())
                .phone(u.getPhone())
                .themePreference(u.getThemePreference())
                .avatar("https://res.cloudinary.com/demo/image/upload/sample")
                .avatarPublicId(u.getAvatarPublicId())
                .avatarProvider("cloudinary")
                .isOnline(u.isOnline())
                .tokenVersion(u.getTokenVersion())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .lastSeenAt(u.getLastSeenAt())
                .build());
    }
}
