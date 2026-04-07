package com.chatrealtime.repository;

import com.chatrealtime.model.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {
    List<Message> findByRoomIdOrderByTimestampAsc(String roomId);
    List<Message> findByRoomIdAndTimestampBeforeOrderByTimestampDesc(String roomId, LocalDateTime before);
}

