package com.chatrealtime.repository;

import com.chatrealtime.domain.Friendship;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends MongoRepository<Friendship, String> {
    boolean existsByUserIdAAndUserIdB(String userIdA, String userIdB);

    Optional<Friendship> findByUserIdAAndUserIdB(String userIdA, String userIdB);

    List<Friendship> findByUserIdsContaining(String userId);
}
