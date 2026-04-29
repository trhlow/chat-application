package com.chatrealtime.repository;

import com.chatrealtime.domain.Room;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends MongoRepository<Room, String> {
    List<Room> findByMemberIdsContaining(String userId);

    List<Room> findByTypeAndMemberIdsContaining(String type, String userId);

    @Query(value = "{ 'memberIds': { '$all': [?0, ?1] } }", exists = true)
    boolean existsRoomSharedBy(String userIdA, String userIdB);
}



