package io.github.tufkan1.projectex.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tufkan1.projectex.alchemy.AlchemyInventory;
import io.github.tufkan1.projectex.alchemy.AlchemyTransactionFailure;
import io.github.tufkan1.projectex.alchemy.AlchemyTransactionTarget;
import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcMatch;
import io.github.tufkan1.projectex.api.emc.EmcSnapshot;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.player.PlayerAlchemyState;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ServerAlchemySessionRegistryTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final EmcMatch COAL = EmcMatch.item(EmcKey.parse("minecraft:coal"));
    private static final EmcSnapshot EMC = new EmcSnapshot(
        4, Map.of(COAL, EmcValue.of(128)), Map.of(COAL, "test"));

    @Test
    void validSessionCommitsAndExactReplayIsRejected() {
        ServerAlchemySessionRegistry sessions = new ServerAlchemySessionRegistry();
        Target target = new Target();
        var handle = sessions.open(target, () -> true, () -> 0);
        AlchemyActionPayload action = action(handle.sessionId(), 0, 1, "minecraft:coal", 1);

        AlchemyResultPayload first = sessions.handle(PLAYER, true, action, EMC, PlayerAlchemyState.EMPTY, 1);
        AlchemyResultPayload replay = sessions.handle(PLAYER, true, action, EMC, PlayerAlchemyState.EMPTY, 2);

        assertTrue(first.success());
        assertEquals(EmcValue.of(128), target.player.balance());
        assertEquals(1, target.inventory.count(COAL));
        assertEquals(AlchemyTransactionFailure.REPLAYED_REQUEST, replay.failure().orElseThrow());
        assertEquals(1, target.commitCalls);
    }

    @Test
    void protocolSessionIdentityAndMalformedOperationsCannotReachCommit() {
        ServerAlchemySessionRegistry sessions = new ServerAlchemySessionRegistry();
        Target target = new Target();
        var handle = sessions.open(target, () -> true, () -> 0);

        assertFailure(sessions, new AlchemyActionPayload(99, handle.sessionId(), 0, 1,
            "minecraft:coal", 1, 4), AlchemyTransactionFailure.UNSUPPORTED_PROTOCOL);
        assertFailure(sessions, action(handle.sessionId() + 1, 0, 1, "minecraft:coal", 1),
            AlchemyTransactionFailure.SESSION_INVALID);
        assertFailure(sessions, action(handle.sessionId(), 0, 99, "minecraft:coal", 1),
            AlchemyTransactionFailure.MALFORMED_REQUEST);
        assertFailure(sessions, action(handle.sessionId(), 1, 1, "not an id", 1),
            AlchemyTransactionFailure.MALFORMED_REQUEST);
        assertEquals(0, target.commitCalls);
    }

    @Test
    void closedUnauthorizedAndDistantSessionsAreRejected() {
        ServerAlchemySessionRegistry sessions = new ServerAlchemySessionRegistry();
        Target target = new Target();
        var unauthorized = sessions.open(target, () -> false, () -> 0);
        assertFailure(sessions, action(unauthorized.sessionId(), 0, 1, "minecraft:coal", 1),
            AlchemyTransactionFailure.SESSION_INVALID);

        var distant = sessions.open(target, () -> true, () -> 65);
        assertFailure(sessions, action(distant.sessionId(), 0, 1, "minecraft:coal", 1),
            AlchemyTransactionFailure.TOO_FAR);

        sessions.close(PLAYER);
        assertFalse(sessions.isOpen(distant));
        assertFailure(sessions, action(distant.sessionId(), 1, 1, "minecraft:coal", 1),
            AlchemyTransactionFailure.SESSION_INVALID);
        assertEquals(0, target.commitCalls);
    }

    @Test
    void malformedWireTrafficIsRateLimitedBeforeSessionValidation() {
        ServerAlchemySessionRegistry sessions = new ServerAlchemySessionRegistry();
        AlchemyActionPayload invalid = action(123, 0, 99, "not an id", 1);
        for (int index = 0; index < NetworkRequestLimiter.MAX_REQUESTS_PER_WINDOW; index++) {
            AlchemyResultPayload result = sessions.handle(
                PLAYER, true, invalid, EMC, PlayerAlchemyState.EMPTY, index);
            assertEquals(AlchemyTransactionFailure.SESSION_INVALID, result.failure().orElseThrow());
        }
        AlchemyResultPayload limited = sessions.handle(
            PLAYER, true, invalid, EMC, PlayerAlchemyState.EMPTY, 40);
        assertEquals(AlchemyTransactionFailure.RATE_LIMITED, limited.failure().orElseThrow());
    }

    @Test
    void failingServerAccessSupplierClosesTheSessionSafely() {
        ServerAlchemySessionRegistry sessions = new ServerAlchemySessionRegistry();
        Target target = new Target();
        var handle = sessions.open(target, () -> {
            throw new IllegalStateException("menu removed");
        }, () -> 0);

        assertFailure(sessions, action(handle.sessionId(), 0, 1, "minecraft:coal", 1),
            AlchemyTransactionFailure.SESSION_INVALID);
        assertFalse(sessions.isOpen(handle));
        assertEquals(0, target.commitCalls);
    }

    private static void assertFailure(
        ServerAlchemySessionRegistry sessions,
        AlchemyActionPayload action,
        AlchemyTransactionFailure expected
    ) {
        AlchemyResultPayload result = sessions.handle(
            PLAYER, true, action, EMC, PlayerAlchemyState.EMPTY, action.requestId());
        assertFalse(result.success());
        assertEquals(expected, result.failure().orElseThrow());
    }

    private static AlchemyActionPayload action(
        long session,
        long request,
        int operation,
        String item,
        int count
    ) {
        return new AlchemyActionPayload(1, session, request, operation, item, count, 4);
    }

    private static final class Target implements AlchemyTransactionTarget {
        private PlayerAlchemyState player = PlayerAlchemyState.EMPTY;
        private AlchemyInventory inventory = new AlchemyInventory(64, Map.of(COAL, 2));
        private int commitCalls;

        @Override
        public UUID playerId() {
            return PLAYER;
        }

        @Override
        public PlayerAlchemyState playerState() {
            return player;
        }

        @Override
        public AlchemyInventory inventory() {
            return inventory;
        }

        @Override
        public boolean commit(
            PlayerAlchemyState expectedPlayer,
            AlchemyInventory expectedInventory,
            PlayerAlchemyState newPlayer,
            AlchemyInventory newInventory
        ) {
            commitCalls++;
            if (!player.equals(expectedPlayer) || !inventory.equals(expectedInventory)) {
                return false;
            }
            player = newPlayer;
            inventory = newInventory;
            return true;
        }
    }
}
