package io.github.tufkan1.projectex.alchemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcMatch;
import io.github.tufkan1.projectex.api.emc.EmcSnapshot;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.player.PlayerAlchemyState;
import java.math.BigInteger;
import java.util.Map;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class AlchemyTransactionServiceTest {
    private static final long REVISION = 7;
    private static final EmcMatch DIAMOND = EmcMatch.item(EmcKey.parse("minecraft:diamond"));
    private static final EmcMatch COAL = EmcMatch.item(EmcKey.parse("minecraft:coal"));
    private static final EmcSnapshot EMC = new EmcSnapshot(
        REVISION,
        Map.of(DIAMOND, EmcValue.of(8192), COAL, EmcValue.of(128)),
        Map.of(DIAMOND, "test", COAL, "test")
    );

    @Test
    void learnRequiresPossessionAndDoesNotConsumeTheItem() {
        AlchemyInventory inventory = inventory(64, DIAMOND, 1);

        AlchemyTransactionResult result = execute(
            PlayerAlchemyState.EMPTY, inventory, new AlchemyTransaction.Learn(DIAMOND, REVISION));

        assertTrue(result.success());
        assertTrue(result.player().knows(DIAMOND.item()));
        assertEquals(inventory, result.inventory());
        assertEquals(EmcValue.of(8192), result.unitValue());
    }

    @Test
    void burnConsumesItemsCreditsServerResolvedEmcAndLearns() {
        AlchemyTransactionResult result = execute(
            PlayerAlchemyState.EMPTY,
            inventory(64, COAL, 3),
            new AlchemyTransaction.Burn(COAL, 2, REVISION)
        );

        assertTrue(result.success());
        assertEquals(1, result.inventory().count(COAL));
        assertEquals(EmcValue.of(256), result.player().balance());
        assertTrue(result.player().knows(COAL.item()));
    }

    @Test
    void createDebitsBalanceAndAddsItemsAtomically() {
        PlayerAlchemyState player = PlayerAlchemyState.EMPTY.credit(EmcValue.of(20_000)).learn(DIAMOND.item());

        AlchemyTransactionResult result = execute(
            player, new AlchemyInventory(64, Map.of()), new AlchemyTransaction.Create(DIAMOND, 2, REVISION));

        assertTrue(result.success());
        assertEquals(2, result.inventory().count(DIAMOND));
        assertEquals(EmcValue.of(3616), result.player().balance());
    }

    @Test
    void everyRejectionReturnsTheExactOriginalObjects() {
        PlayerAlchemyState player = PlayerAlchemyState.EMPTY;
        AlchemyInventory inventory = inventory(1, COAL, 1);
        Map<AlchemyTransaction, AlchemyTransactionFailure> rejected = Map.of(
            new AlchemyTransaction.Learn(DIAMOND, REVISION), AlchemyTransactionFailure.ITEM_NOT_PRESENT,
            new AlchemyTransaction.Burn(COAL, 2, REVISION), AlchemyTransactionFailure.ITEM_NOT_PRESENT,
            new AlchemyTransaction.Create(DIAMOND, 1, REVISION), AlchemyTransactionFailure.ITEM_NOT_LEARNED,
            new AlchemyTransaction.Burn(COAL, 1, REVISION - 1), AlchemyTransactionFailure.STALE_EMC_REVISION,
            new AlchemyTransaction.Burn(COAL, 0, REVISION), AlchemyTransactionFailure.INVALID_COUNT
        );

        rejected.forEach((request, expected) -> {
            AlchemyTransactionResult result = execute(player, inventory, request);
            assertFalse(result.success());
            assertEquals(expected, result.failure());
            assertEquals(player, result.player());
            assertEquals(inventory, result.inventory());
        });
    }

    @Test
    void createRejectsInsufficientBalanceAndFullInventoryWithoutCharging() {
        PlayerAlchemyState poor = PlayerAlchemyState.EMPTY.learn(DIAMOND.item());
        AlchemyTransaction request = new AlchemyTransaction.Create(DIAMOND, 1, REVISION);

        assertEquals(AlchemyTransactionFailure.INSUFFICIENT_EMC,
            execute(poor, new AlchemyInventory(64, Map.of()), request).failure());

        PlayerAlchemyState funded = poor.credit(EmcValue.of(8192));
        AlchemyInventory full = inventory(1, COAL, 1);
        AlchemyTransactionResult result = execute(funded, full, request);
        assertEquals(AlchemyTransactionFailure.INVENTORY_FULL, result.failure());
        assertEquals(funded, result.player());
        assertEquals(full, result.inventory());
    }

    @Test
    void rejectsUnknownZeroAndOverLimitRequests() {
        EmcMatch stick = EmcMatch.item(EmcKey.parse("minecraft:stick"));
        for (int count = -10; count <= 100; count++) {
            if (count >= 1 && count <= AlchemyTransactionService.MAX_REQUEST_COUNT) {
                continue;
            }
            AlchemyTransactionResult result = execute(
                PlayerAlchemyState.EMPTY, inventory(64, COAL, 1),
                new AlchemyTransaction.Burn(COAL, count, REVISION));
            assertEquals(AlchemyTransactionFailure.INVALID_COUNT, result.failure());
        }
        assertEquals(AlchemyTransactionFailure.UNKNOWN_EMC_VALUE,
            execute(PlayerAlchemyState.EMPTY, new AlchemyInventory(64, Map.of()),
                new AlchemyTransaction.Learn(stick, REVISION)).failure());
    }

    @Test
    void rejectsCreditPastThePersistentBalanceSafetyLimit() {
        String maximum = "9".repeat(PlayerAlchemyState.MAX_BALANCE_DIGITS);
        PlayerAlchemyState player = new PlayerAlchemyState(
            new EmcValue(new BigInteger(maximum)), new TreeSet<>());
        AlchemyInventory inventory = inventory(64, COAL, 1);

        AlchemyTransactionResult result = execute(
            player, inventory, new AlchemyTransaction.Burn(COAL, 1, REVISION));

        assertEquals(AlchemyTransactionFailure.BALANCE_LIMIT_EXCEEDED, result.failure());
        assertEquals(player, result.player());
        assertEquals(inventory, result.inventory());
    }

    @Test
    void burnThenCreateConservesItemsAndEmcAcrossRepeatedCycles() {
        PlayerAlchemyState player = PlayerAlchemyState.EMPTY;
        AlchemyInventory inventory = inventory(64, COAL, 64);
        for (int cycle = 0; cycle < 100; cycle++) {
            AlchemyTransactionResult burned = execute(
                player, inventory, new AlchemyTransaction.Burn(COAL, 64, REVISION));
            assertTrue(burned.success());
            AlchemyTransactionResult created = execute(
                burned.player(), burned.inventory(), new AlchemyTransaction.Create(COAL, 64, REVISION));
            assertTrue(created.success());
            player = created.player();
            inventory = created.inventory();
        }

        assertEquals(EmcValue.ZERO, player.balance());
        assertEquals(64, inventory.count(COAL));
    }

    private static AlchemyTransactionResult execute(
        PlayerAlchemyState player,
        AlchemyInventory inventory,
        AlchemyTransaction request
    ) {
        return AlchemyTransactionService.execute(player, inventory, request, EMC);
    }

    private static AlchemyInventory inventory(int capacity, EmcMatch item, int count) {
        return new AlchemyInventory(capacity, Map.of(item, count));
    }
}
