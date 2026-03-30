package com.chatrealtime.repository;

import com.chatrealtime.model.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

public interface MessageRepository extends MongoRepository<Message, String>{
}