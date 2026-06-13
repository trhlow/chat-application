package com.chatrealtime.config;

import com.mongodb.client.model.IndexOptions;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class RoomIndexInitializer implements InitializingBean {
    private static final String ROOMS_COLLECTION = "rooms";
    private static final String DIRECT_ROOM_INDEX = "uk_direct_room_key";

    private final MongoTemplate mongoTemplate;

    @Override
    public void afterPropertiesSet() {
        mongoTemplate.getCollection(ROOMS_COLLECTION).createIndex(
                new Document("type", 1).append("directKey", 1),
                new IndexOptions()
                        .name(DIRECT_ROOM_INDEX)
                        .unique(true)
                        .partialFilterExpression(new Document("type", "direct")
                                .append("directKey", new Document("$type", "string")))
        );
    }
}
