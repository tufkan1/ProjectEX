package io.github.tufkan1.projectex.network;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Coarse pre-session limiter so malformed protocol traffic cannot bypass domain throttling. */
final class NetworkRequestLimiter {
    static final int MAX_REQUESTS_PER_WINDOW = 40;
    static final long WINDOW_MILLIS = 1_000;

    private final Map<UUID, ArrayDeque<Long>> requests = new HashMap<>();

    synchronized boolean allow(UUID playerId, long monotonicMillis) {
        ArrayDeque<Long> playerRequests = requests.computeIfAbsent(playerId, ignored -> new ArrayDeque<>());
        long threshold = monotonicMillis - WINDOW_MILLIS;
        while (!playerRequests.isEmpty() && playerRequests.getFirst() <= threshold) {
            playerRequests.removeFirst();
        }
        if (playerRequests.size() >= MAX_REQUESTS_PER_WINDOW) {
            return false;
        }
        playerRequests.addLast(monotonicMillis);
        return true;
    }

    synchronized void disconnect(UUID playerId) {
        requests.remove(playerId);
    }
}
