package com.chatrealtime.repository;

import com.chatrealtime.domain.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, String> {
    boolean existsByUserIdAAndUserIdB(String userIdA, String userIdB);

    Optional<Friendship> findByUserIdAAndUserIdB(String userIdA, String userIdB);

    @Query("select friendship from Friendship friendship join friendship.userIds userId where userId = :userId")
    List<Friendship> findByUserIdsContaining(@Param("userId") String userId);
}
