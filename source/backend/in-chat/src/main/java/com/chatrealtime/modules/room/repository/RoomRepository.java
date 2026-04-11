package com.chatrealtime.modules.room.repository;

import com.chatrealtime.modules.room.model.Room;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends MongoRepository<Room, String> {
    List<Room> findByMemberIdsContaining(String userId);
}



