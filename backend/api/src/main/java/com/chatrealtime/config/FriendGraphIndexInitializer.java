package com.chatrealtime.config;

import com.chatrealtime.domain.FriendRequest;
import com.chatrealtime.domain.Friendship;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class FriendGraphIndexInitializer implements InitializingBean {
    private static final String FRIENDSHIPS_COLLECTION = "friendships";
    private static final String FRIEND_REQUESTS_COLLECTION = "friend_requests";
    private static final String FRIENDSHIP_INDEX = "uk_friendship_pair";
    private static final String FRIEND_REQUEST_INDEX = "uk_friend_request_pair_status";

    private final MongoTemplate mongoTemplate;

    @Override
    public void afterPropertiesSet() {
        ensureNoDuplicateFriendships();
        ensureNoDuplicateFriendRequests();
        ensureIndexes();
    }

    private void ensureNoDuplicateFriendships() {
        assertNoDuplicates(
                FRIENDSHIPS_COLLECTION,
                new Document("userIdA", "$userIdA").append("userIdB", "$userIdB"),
                "Duplicate friendship pair exists; clean duplicates before creating unique index"
        );
    }

    private void ensureNoDuplicateFriendRequests() {
        assertNoDuplicates(
                FRIEND_REQUESTS_COLLECTION,
                new Document("userIdA", "$userIdA").append("userIdB", "$userIdB").append("status", "$status"),
                "Duplicate friend request pair/status exists; clean duplicates before creating unique index"
        );
    }

    private void assertNoDuplicates(String collection, Document groupId, String message) {
        List<Document> pipeline = List.of(
                new Document("$match", new Document(groupId.keySet().stream()
                        .collect(Document::new, (doc, key) -> doc.append(key, new Document("$ne", null)), Document::putAll))),
                new Document("$group", new Document("_id", groupId).append("count", new Document("$sum", 1))),
                new Document("$match", new Document("count", new Document("$gt", 1))),
                new Document("$limit", 1)
        );
        if (mongoTemplate.getCollection(collection).aggregate(pipeline).first() != null) {
            throw new IllegalStateException(message);
        }
    }

    private void ensureIndexes() {
        mongoTemplate.indexOps(Friendship.class)
                .createIndex(new Index()
                        .on("userIdA", Sort.Direction.ASC)
                        .on("userIdB", Sort.Direction.ASC)
                        .unique()
                        .named(FRIENDSHIP_INDEX));

        mongoTemplate.indexOps(FriendRequest.class)
                .createIndex(new Index()
                        .on("userIdA", Sort.Direction.ASC)
                        .on("userIdB", Sort.Direction.ASC)
                        .on("status", Sort.Direction.ASC)
                        .unique()
                        .named(FRIEND_REQUEST_INDEX));
    }
}
