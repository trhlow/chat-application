package com.chatrealtime.repository;

import com.chatrealtime.domain.FriendRequest;
import com.chatrealtime.domain.FriendRequestStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FriendRequestRepository extends MongoRepository<FriendRequest, String> {
    boolean existsByRequesterIdAndReceiverIdAndStatus(
            String requesterId,
            String receiverId,
            FriendRequestStatus status
    );

    List<FriendRequest> findByReceiverIdAndStatusOrderByCreatedAtDesc(
            String receiverId,
            FriendRequestStatus status
    );

    List<FriendRequest> findByRequesterIdAndStatusOrderByCreatedAtDesc(
            String requesterId,
            FriendRequestStatus status
    );
}
