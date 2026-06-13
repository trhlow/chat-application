package com.chatrealtime.config;

import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
@Order(0)
@RequiredArgsConstructor
public class MongoTransactionStartupValidator implements ApplicationRunner {

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) {
        Document hello;
        try {
            hello = mongoTemplate.executeCommand(new Document("hello", 1));
        } catch (DataAccessException exception) {
            throw new IllegalStateException("MongoDB transaction capability check failed", exception);
        }

        if (!supportsTransactions(hello)) {
            throw new IllegalStateException(
                    "MongoDB transactions require a replica set or sharded cluster in prod"
            );
        }
    }

    private boolean supportsTransactions(Document hello) {
        return hello.containsKey("setName") || "isdbgrid".equals(hello.getString("msg"));
    }
}
