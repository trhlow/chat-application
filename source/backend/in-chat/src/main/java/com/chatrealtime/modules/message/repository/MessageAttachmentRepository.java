package com.chatrealtime.modules.message.repository;

import com.chatrealtime.modules.message.model.MessageAttachment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MessageAttachmentRepository extends MongoRepository<MessageAttachment, String> {
    List<MessageAttachment> findByMessageId(String messageId);
    List<MessageAttachment> findByMessageIdIn(Collection<String> messageIds);
}
