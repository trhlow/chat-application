package com.chatrealtime.repository;

import com.chatrealtime.domain.GroupJoinRequest;
import com.chatrealtime.domain.GroupJoinRequestStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupJoinRequestRepository extends MongoRepository<GroupJoinRequest, String> {
    boolean existsByRoomIdAndTargetUserIdAndStatus(String roomId, String targetUserId, GroupJoinRequestStatus status);

    List<GroupJoinRequest> findByRoomIdAndStatusOrderByCreatedAtDesc(String roomId, GroupJoinRequestStatus status);

    Optional<GroupJoinRequest> findByIdAndRoomId(String id, String roomId);
}
