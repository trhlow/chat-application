package com.chatrealtime.modules.user.storage;

import org.springframework.web.multipart.MultipartFile;

public interface AvatarStorageService {
    AvatarUploadResult uploadAvatar(String userId, MultipartFile file);
    void deleteAvatar(String provider, String publicId);
}
