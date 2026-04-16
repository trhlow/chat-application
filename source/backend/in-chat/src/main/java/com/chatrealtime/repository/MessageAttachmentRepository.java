package com.chatrealtime.repository;

import com.chatrealtime.domain.MessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, String> {
    List<MessageAttachment> findByMessageId(String messageId);

    List<MessageAttachment> findByMessageIdIn(Collection<String> messageIds);
}
