package com.chatrealtime.modules.message.repository;

import com.chatrealtime.modules.message.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {
    Page<Message> findByRoomIdOrderByTimestampDesc(String roomId, Pageable pageable);
    Page<Message> findByRoomIdAndTimestampBeforeOrderByTimestampDesc(String roomId, LocalDateTime before, Pageable pageable);
}



