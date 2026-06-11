package com.chatrealtime.presence;

public interface PresenceStateStore {
    void markOnline(String userId);

    void markOffline(String userId);

    boolean registerSession(String userId, String sessionId);

    boolean unregisterSession(String userId, String sessionId);

    boolean isOnline(String userId);
}
