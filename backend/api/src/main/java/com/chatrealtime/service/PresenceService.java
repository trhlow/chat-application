package com.chatrealtime.service;

public interface PresenceService {
    void markOnline(String userId);

    void markOffline(String userId);

    void markSessionOnline(String userId, String sessionId);

    void markSessionOffline(String userId, String sessionId);

    boolean isUserOnline(String userId);
}
