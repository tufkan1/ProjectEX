package io.github.tufkan1.projectex.alchemy;

import java.util.UUID;

/** Server-computed access facts; none of these values should be accepted from a client payload. */
public record AlchemyRequestContext(
    UUID playerId,
    boolean connected,
    boolean authorizedMenu,
    double distanceSquared,
    long monotonicMillis
) {
    public AlchemyRequestContext {
        java.util.Objects.requireNonNull(playerId, "playerId");
    }
}
