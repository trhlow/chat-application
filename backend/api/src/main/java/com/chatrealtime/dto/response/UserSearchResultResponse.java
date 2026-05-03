package com.chatrealtime.dto.response;

/**
 * Minimal fields for username search; no email or other PII.
 */
public record UserSearchResultResponse(
        String id,
        String username,
        String displayName,
        String avatarEndpoint,
        @Deprecated(forRemoval = true)
        String avatar
) {
}
