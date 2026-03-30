package com.chatrealtime.repository;

import com.chatrealtime.model.Room;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomRequest extends MongoRepository<Room, String>{
}