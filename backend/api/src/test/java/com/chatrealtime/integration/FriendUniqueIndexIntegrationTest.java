package com.chatrealtime.integration;

import com.chatrealtime.domain.FriendRequest;
import com.chatrealtime.domain.FriendRequestStatus;
import com.chatrealtime.domain.Friendship;
import com.chatrealtime.repository.FriendRequestRepository;
import com.chatrealtime.repository.FriendshipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class FriendUniqueIndexIntegrationTest {
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private FriendshipRepository friendshipRepository;
    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @BeforeEach
    void recreateCollectionsWithUniqueIndexes() {
        mongoTemplate.dropCollection("friendships");
        mongoTemplate.dropCollection("friend_requests");

        mongoTemplate.indexOps("friendships")
                .ensureIndex(new Index()
                        .on("userIdA", Sort.Direction.ASC)
                        .on("userIdB", Sort.Direction.ASC)
                        .unique()
                        .named("uk_friendship_pair"));

        mongoTemplate.indexOps("friend_requests")
                .ensureIndex(new Index()
                        .on("userIdA", Sort.Direction.ASC)
                        .on("userIdB", Sort.Direction.ASC)
                        .on("status", Sort.Direction.ASC)
                        .unique()
                        .named("uk_friend_request_pair_status"));
    }

    @Test
    void friendUniqueIndexes_ShouldExistWithExpectedNames() {
        assertThat(mongoTemplate.indexOps("friendships").getIndexInfo())
                .anySatisfy(index -> assertThat(index.getName()).isEqualTo("uk_friendship_pair"));

        assertThat(mongoTemplate.indexOps("friend_requests").getIndexInfo())
                .anySatisfy(index -> assertThat(index.getName()).isEqualTo("uk_friend_request_pair_status"));
    }

    @Test
    void friendshipDuplicateCanonicalPair_ShouldThrowDuplicateKeyException() {
        String userA = uniqueUserId("a");
        String userB = uniqueUserId("b");
        friendshipRepository.save(friendship("f1", userA, userB));

        assertThatThrownBy(() -> friendshipRepository.save(friendship("f2", userA, userB)))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void friendRequestDuplicateCanonicalPairAndStatus_ShouldThrowDuplicateKeyException() {
        String userA = uniqueUserId("a");
        String userB = uniqueUserId("b");
        friendRequestRepository.save(friendRequest("r1", userA, userB, userA, userB, FriendRequestStatus.PENDING));

        assertThatThrownBy(() -> friendRequestRepository.save(
                friendRequest("r2", userB, userA, userA, userB, FriendRequestStatus.PENDING)
        )).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void friendRequestSamePairWithDifferentStatus_ShouldBeAllowed() {
        String userA = uniqueUserId("a");
        String userB = uniqueUserId("b");
        friendRequestRepository.save(friendRequest("r1", userA, userB, userA, userB, FriendRequestStatus.PENDING));

        FriendRequest saved = friendRequestRepository.save(
                friendRequest("r2", userB, userA, userA, userB, FriendRequestStatus.REJECTED)
        );

        assertThat(saved.getId()).startsWith("r2-");
    }

    private Friendship friendship(String id, String userA, String userB) {
        return Friendship.builder()
                .id(id + "-" + UUID.randomUUID())
                .userIdA(userA)
                .userIdB(userB)
                .userIds(List.of(userA, userB))
                .createdAt(Instant.now())
                .build();
    }

    private FriendRequest friendRequest(
            String id,
            String requesterId,
            String receiverId,
            String userIdA,
            String userIdB,
            FriendRequestStatus status
    ) {
        return FriendRequest.builder()
                .id(id + "-" + UUID.randomUUID())
                .requesterId(requesterId)
                .receiverId(receiverId)
                .userIdA(userIdA)
                .userIdB(userIdB)
                .status(status)
                .createdAt(Instant.now())
                .build();
    }

    private String uniqueUserId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
