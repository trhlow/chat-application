package com.chatrealtime.service;

/**
 * Result of atomically revoking the current refresh token and issuing a replacement.
 */
public record RefreshRotationResult(String newRefreshToken, String userId) {
}
