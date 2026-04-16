package com.chatrealtime.service;

public interface PresenceService {
    void markOnline(String userId);

    void markOffline(String userId);
}
