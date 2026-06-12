package com.chatrealtime.repository;

import com.chatrealtime.domain.UserBlock;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBlockRepository extends MongoRepository<UserBlock, String> {
    boolean existsByBlockerIdAndBlockedId(String blockerId, String blockedId);

    Optional<UserBlock> findByBlockerIdAndBlockedId(String blockerId, String blockedId);

    List<UserBlock> findByBlockerIdOrderByCreatedAtDesc(String blockerId);

    @Query(value = "{ '$or': [ { 'blockerId': ?0, 'blockedId': ?1 }, { 'blockerId': ?1, 'blockedId': ?0 } ] }", exists = true)
    boolean existsBetweenUsers(String userIdA, String userIdB);
}
