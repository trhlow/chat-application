package com.chatrealtime.modules.message.storage;

import org.springframework.web.multipart.MultipartFile;

public interface MessageAttachmentStorageService {
    StoredMessageAttachment store(String userId, MultipartFile file);
}
