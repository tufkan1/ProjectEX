package io.github.tufkan1.projectex.alchemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AlchemyRequestGuardTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void rejectsDisconnectedUnauthorizedAndDistantContexts() {
        AlchemyRequestGuard guard = new AlchemyRequestGuard();

        assertEquals(AlchemyTransactionFailure.SESSION_INVALID,
            guard.validate(context(false, true, 0, 1)).orElseThrow());
        assertEquals(AlchemyTransactionFailure.SESSION_INVALID,
            guard.validate(context(true, false, 0, 1)).orElseThrow());
        assertEquals(AlchemyTransactionFailure.TOO_FAR,
            guard.validate(context(true, true, 65, 1)).orElseThrow());
        assertEquals(AlchemyTransactionFailure.TOO_FAR,
            guard.validate(context(true, true, Double.NaN, 1)).orElseThrow());
    }

    @Test
    void rateLimitsPerPlayerAndRecoversAfterTheWindow() {
        AlchemyRequestGuard guard = new AlchemyRequestGuard();
        for (int index = 0; index < AlchemyRequestGuard.MAX_REQUESTS_PER_WINDOW; index++) {
            assertTrue(guard.validate(context(true, true, 0, index)).isEmpty());
        }
        assertEquals(AlchemyTransactionFailure.RATE_LIMITED,
            guard.validate(context(true, true, 0, 20)).orElseThrow());
        assertTrue(guard.validate(context(true, true, 0, 1_001)).isEmpty());

        guard.disconnect(PLAYER);
        assertTrue(guard.validate(context(true, true, 0, 1_001)).isEmpty());
    }

    private static AlchemyRequestContext context(
        boolean connected,
        boolean menu,
        double distanceSquared,
        long time
    ) {
        return new AlchemyRequestContext(PLAYER, connected, menu, distanceSquared, time);
    }
}
