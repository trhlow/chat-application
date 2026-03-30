package com.chatrealtime.repository;

import com.chatrealtime.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRequest extends MongoRepository<Notification, String>{
}