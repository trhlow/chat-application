package com.chatrealtime.config;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MongoTransactionStartupValidatorTest {

    private final MongoTemplate mongoTemplate = mock(MongoTemplate.class);
    private final MongoTransactionStartupValidator validator = new MongoTransactionStartupValidator(mongoTemplate);

    @Test
    void run_whenReplicaSet_shouldPass() {
        when(mongoTemplate.executeCommand(new Document("hello", 1)))
                .thenReturn(new Document("setName", "rs0"));

        assertThatCode(() -> validator.run(null)).doesNotThrowAnyException();
    }

    @Test
    void run_whenShardedCluster_shouldPass() {
        when(mongoTemplate.executeCommand(new Document("hello", 1)))
                .thenReturn(new Document("msg", "isdbgrid"));

        assertThatCode(() -> validator.run(null)).doesNotThrowAnyException();
    }

    @Test
    void run_whenStandaloneMongo_shouldFailFast() {
        when(mongoTemplate.executeCommand(new Document("hello", 1)))
                .thenReturn(new Document("isWritablePrimary", true));

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(org.springframework.context.ApplicationContextException.class)
                .hasMessageContaining("MongoDB Replica Set is required");
    }
}
