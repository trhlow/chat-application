package com.chatrealtime.repository;

import com.chatrealtime.domain.Room;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends MongoRepository<Room, String> {
    List<Room> findByMemberIdsContaining(String userId);

    List<Room> findByTypeAndMemberIdsContaining(String type, String userId);
}



