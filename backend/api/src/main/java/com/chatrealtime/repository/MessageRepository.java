package com.chatrealtime.repository;

import com.chatrealtime.domain.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {
    Page<Message> findByRoomIdOrderByTimestampDesc(String roomId, Pageable pageable);
    Page<Message> findByRoomIdAndTimestampBeforeOrderByTimestampDesc(String roomId, LocalDateTime before, Pageable pageable);
    java.util.List<Message> findByRoomId(String roomId);
    Optional<Message> findByRoomIdAndSenderIdAndClientMessageId(String roomId, String senderId, String clientMessageId);
}



