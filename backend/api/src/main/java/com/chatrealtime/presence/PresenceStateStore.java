package com.chatrealtime.presence;

public interface PresenceStateStore {
    void markOnline(String userId);

    void markOffline(String userId);

    boolean isOnline(String userId);
}
