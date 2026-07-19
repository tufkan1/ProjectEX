package io.github.tufkan1.projectex.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tufkan1.projectex.alchemy.AlchemyTransactionFailure;
import io.github.tufkan1.projectex.network.AlchemyActionPayload;
import io.github.tufkan1.projectex.network.AlchemyResultPayload;
import io.github.tufkan1.projectex.network.AlchemySessionPayload;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class ClientAlchemySessionStateTest {
    @Test
    void opensOnlyValidServerSessionsAndResetsRequestSequence() {
        ClientAlchemySessionState state = new ClientAlchemySessionState();

        assertFalse(state.open(new AlchemySessionPayload(99, 1, 2, "0", 0)));
        assertFalse(state.snapshot().active());
        assertTrue(state.open(new AlchemySessionPayload(1, 10, 2, "128", 1)));

        AlchemyActionPayload first = state.nextAction(1, "minecraft:coal", 1).orElseThrow();
        AlchemyActionPayload second = state.nextAction(2, "minecraft:coal", 1).orElseThrow();
        assertEquals(0, first.requestId());
        assertEquals(1, second.requestId());
        assertEquals(10, first.sessionId());
        assertEquals(2, first.emcRevision());
        assertTrue(state.nextAction(99, "minecraft:coal", 1).isEmpty());
        assertEquals(2, state.snapshot().nextRequestId());
    }

    @Test
    void acceptsOnlyExpectedMonotonicAuthoritativeResults() {
        ClientAlchemySessionState state = new ClientAlchemySessionState();
        state.open(new AlchemySessionPayload(1, 10, 2, "128", 1));
        state.nextAction(1, "minecraft:coal", 1).orElseThrow();
        state.nextAction(1, "minecraft:coal", 1).orElseThrow();

        AlchemyResultPayload newer = result(10, 1, true, AlchemyTransactionFailure.NONE, 3, "256", 1);
        assertTrue(state.accept(newer));
        assertEquals(BigInteger.valueOf(256), state.snapshot().balance());
        assertEquals(3, state.snapshot().emcRevision());

        assertFalse(state.accept(result(10, 0, true, AlchemyTransactionFailure.NONE, 2, "128", 1)));
        assertFalse(state.accept(result(11, 1, true, AlchemyTransactionFailure.NONE, 3, "999", 1)));
        assertFalse(state.accept(result(10, 2, true, AlchemyTransactionFailure.NONE, 3, "999", 1)));
        assertEquals(BigInteger.valueOf(256), state.snapshot().balance());
    }

    @Test
    void authoritativeFailureStillRefreshesRevisionAndBalance() {
        ClientAlchemySessionState state = new ClientAlchemySessionState();
        state.open(new AlchemySessionPayload(1, 10, 2, "128", 1));
        state.nextAction(2, "minecraft:diamond", 1).orElseThrow();

        assertTrue(state.accept(result(
            10, 0, false, AlchemyTransactionFailure.STALE_EMC_REVISION, 4, "128", 1)));
        assertEquals(AlchemyTransactionFailure.STALE_EMC_REVISION,
            state.snapshot().lastFailure().orElseThrow());
        assertEquals(4, state.snapshot().emcRevision());

        state.close();
        assertFalse(state.snapshot().active());
        assertFalse(state.accept(result(10, 1, true, AlchemyTransactionFailure.NONE, 4, "0", 0)));
    }

    private static AlchemyResultPayload result(
        long session,
        long request,
        boolean success,
        AlchemyTransactionFailure failure,
        long revision,
        String balance,
        int knowledge
    ) {
        return new AlchemyResultPayload(
            1, session, request, success, failure.ordinal(), revision, balance, knowledge);
    }
}
