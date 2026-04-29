package com.chatrealtime.util;

/**
 * Canonical ordering for friendship repository keys (userIdA &lt;= userIdB lexicographically).
 */
public final class UserIdPair {

    private UserIdPair() {
    }

    public record Ordered(String userIdA, String userIdB) {
    }

    public static Ordered order(String userId1, String userId2) {
        if (userId1.compareTo(userId2) <= 0) {
            return new Ordered(userId1, userId2);
        }
        return new Ordered(userId2, userId1);
    }
}
