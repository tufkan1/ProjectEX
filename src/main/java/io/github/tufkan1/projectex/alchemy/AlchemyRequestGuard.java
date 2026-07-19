package io.github.tufkan1.projectex.alchemy;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Per-player sliding-window rate limit plus server-owned menu and distance validation. */
public final class AlchemyRequestGuard {
    public static final double MAX_DISTANCE_SQUARED = 64.0;
    public static final int MAX_REQUESTS_PER_WINDOW = 20;
    public static final long WINDOW_MILLIS = 1_000;

    private final Map<UUID, ArrayDeque<Long>> requests = new HashMap<>();

    public synchronized Optional<AlchemyTransactionFailure> validate(AlchemyRequestContext context) {
        if (!context.connected() || !context.authorizedMenu()) {
            return Optional.of(AlchemyTransactionFailure.SESSION_INVALID);
        }
        if (!Double.isFinite(context.distanceSquared())
            || context.distanceSquared() < 0
            || context.distanceSquared() > MAX_DISTANCE_SQUARED) {
            return Optional.of(AlchemyTransactionFailure.TOO_FAR);
        }
        ArrayDeque<Long> playerRequests = requests.computeIfAbsent(context.playerId(), ignored -> new ArrayDeque<>());
        long threshold = context.monotonicMillis() - WINDOW_MILLIS;
        while (!playerRequests.isEmpty() && playerRequests.getFirst() <= threshold) {
            playerRequests.removeFirst();
        }
        if (playerRequests.size() >= MAX_REQUESTS_PER_WINDOW) {
            return Optional.of(AlchemyTransactionFailure.RATE_LIMITED);
        }
        playerRequests.addLast(context.monotonicMillis());
        return Optional.empty();
    }

    public synchronized void disconnect(UUID playerId) {
        requests.remove(playerId);
    }
}
