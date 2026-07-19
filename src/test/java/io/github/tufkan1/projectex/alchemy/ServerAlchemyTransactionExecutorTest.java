package io.github.tufkan1.projectex.alchemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcMatch;
import io.github.tufkan1.projectex.api.emc.EmcSnapshot;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.player.PlayerAlchemyState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ServerAlchemyTransactionExecutorTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final EmcMatch COAL = EmcMatch.item(EmcKey.parse("minecraft:coal"));
    private static final EmcSnapshot EMC = new EmcSnapshot(
        1, Map.of(COAL, EmcValue.of(128)), Map.of(COAL, "test"));

    @Test
    void commitsBothReplacementValuesAndEmitsAnAuditEvent() {
        Target target = new Target(PlayerAlchemyState.EMPTY, new AlchemyInventory(64, Map.of(COAL, 2)));
        List<AlchemyAuditEvent> audit = new ArrayList<>();
        ServerAlchemyTransactionExecutor executor = new ServerAlchemyTransactionExecutor(
            new AlchemyRequestGuard(), audit::add);

        AlchemyTransactionResult result = executor.execute(
            context(PLAYER), target, new AlchemyTransaction.Burn(COAL, 1, 1), EMC);

        assertTrue(result.success());
        assertEquals(EmcValue.of(128), target.player.balance());
        assertEquals(1, target.inventory.count(COAL));
        assertEquals(1, audit.size());
        assertTrue(audit.getFirst().success());
    }

    @Test
    void compareAndCommitRaceRejectsWithoutWritingEitherValue() {
        Target target = new Target(PlayerAlchemyState.EMPTY, new AlchemyInventory(64, Map.of(COAL, 2)));
        target.rejectCommit = true;
        ServerAlchemyTransactionExecutor executor = new ServerAlchemyTransactionExecutor(
            new AlchemyRequestGuard(), AlchemyAuditSink.NOOP);

        AlchemyTransactionResult result = executor.execute(
            context(PLAYER), target, new AlchemyTransaction.Burn(COAL, 1, 1), EMC);

        assertFalse(result.success());
        assertEquals(AlchemyTransactionFailure.STATE_CHANGED, result.failure());
        assertEquals(PlayerAlchemyState.EMPTY, target.player);
        assertEquals(2, target.inventory.count(COAL));
    }

    @Test
    void refusesAContextForAnotherPlayerBeforeCommit() {
        Target target = new Target(PlayerAlchemyState.EMPTY, new AlchemyInventory(64, Map.of(COAL, 1)));
        ServerAlchemyTransactionExecutor executor = new ServerAlchemyTransactionExecutor(
            new AlchemyRequestGuard(), AlchemyAuditSink.NOOP);

        AlchemyTransactionResult result = executor.execute(
            context(UUID.randomUUID()), target, new AlchemyTransaction.Burn(COAL, 1, 1), EMC);

        assertEquals(AlchemyTransactionFailure.SESSION_INVALID, result.failure());
        assertEquals(0, target.commitCalls);
    }

    @Test
    void auditSinkFailureCannotRollBackOrDuplicateACommittedTransaction() {
        Target target = new Target(PlayerAlchemyState.EMPTY, new AlchemyInventory(64, Map.of(COAL, 1)));
        ServerAlchemyTransactionExecutor executor = new ServerAlchemyTransactionExecutor(
            new AlchemyRequestGuard(), event -> {
                throw new IllegalStateException("audit unavailable");
            });

        AlchemyTransactionResult result = executor.execute(
            context(PLAYER), target, new AlchemyTransaction.Burn(COAL, 1, 1), EMC);

        assertTrue(result.success());
        assertEquals(EmcValue.of(128), target.player.balance());
        assertEquals(0, target.inventory.count(COAL));
        assertEquals(1, target.commitCalls);
    }

    private static AlchemyRequestContext context(UUID player) {
        return new AlchemyRequestContext(player, true, true, 0, 1);
    }

    private static final class Target implements AlchemyTransactionTarget {
        private PlayerAlchemyState player;
        private AlchemyInventory inventory;
        private boolean rejectCommit;
        private int commitCalls;

        private Target(PlayerAlchemyState player, AlchemyInventory inventory) {
            this.player = player;
            this.inventory = inventory;
        }

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
            if (rejectCommit || !player.equals(expectedPlayer) || !inventory.equals(expectedInventory)) {
                return false;
            }
            player = newPlayer;
            inventory = newInventory;
            return true;
        }
    }
}
