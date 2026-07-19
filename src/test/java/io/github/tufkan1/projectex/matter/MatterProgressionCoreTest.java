package io.github.tufkan1.projectex.matter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.storage.CondenserVariant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class MatterProgressionCoreTest {
    @Test
    void defaultTiersAreBoundedAndOrdered() {
        assertTrue(MatterTier.RED.miningSpeed() > MatterTier.DARK.miningSpeed());
        assertTrue(MatterTier.RED.maxAreaBlocks() > MatterTier.DARK.maxAreaBlocks());
        assertTrue(MatterTier.RED.furnaceCookTicks() < MatterTier.DARK.furnaceCookTicks());
        assertTrue(MatterTier.RED.armorDamageReductionCap() <= 0.95);
        assertThrows(IllegalArgumentException.class, () -> new MatterTier(
            "unsafe", 1, 1, 1, 1, 100_000, 0, EmcValue.ZERO,
            0, 0, 0, 0, 1.0
        ));
    }

    @Test
    void areaPlanIsRaycastBoundedProtectionAwareBudgetedAndDeterministic() {
        UUID actor = UUID.randomUUID();
        MatterActionPlanner.Position origin = new MatterActionPlanner.Position(0, 64, 0);
        var denied = new MatterActionPlanner.Position(0, 64, 1);
        var plan = MatterActionPlanner.plan(
            MatterTier.DARK,
            actor,
            origin,
            List.of(
                new MatterActionPlanner.Position(2, 64, 0),
                denied,
                new MatterActionPlanner.Position(1, 64, 0),
                new MatterActionPlanner.Position(99, 64, 0),
                new MatterActionPlanner.Position(1, 64, 0)
            ),
            position -> !position.equals(denied),
            EmcValue.of(64),
            2
        );
        assertEquals(List.of(new MatterActionPlanner.Position(1, 64, 0)), plan.accepted());
        assertEquals(List.of(denied), plan.protectionDenied());
        assertEquals(EmcValue.of(64), plan.emcCost());
        assertEquals(MatterTier.DARK.actionCooldownTicks(), plan.cooldownTicks());
    }

    @Test
    void areaPlanNeverExceedsTierRequestCap() {
        var origin = new MatterActionPlanner.Position(0, 0, 0);
        List<MatterActionPlanner.Position> candidates = java.util.stream.IntStream.range(0, 11 * 11 * 11)
            .mapToObj(index -> new MatterActionPlanner.Position(
                index % 11 - 5, (index / 11) % 11 - 5, index / 121 - 5
            ))
            .toList();
        var plan = MatterActionPlanner.plan(
            MatterTier.RED, UUID.randomUUID(), origin, candidates, ignored -> true,
            EmcValue.of(Long.MAX_VALUE), MatterTier.RED.maxCharge()
        );
        assertEquals(MatterTier.RED.maxAreaBlocks(), plan.accepted().size());
    }

    @Test
    void furnaceCommitsWholeComponentExactResultOrNothing() {
        CondenserVariant result = new CondenserVariant(
            "minecraft:potion", "{\"minecraft:potion_contents\":{\"potion\":\"minecraft:healing\"}}"
        );
        var empty = MatterFurnaceTransaction.OutputSlot.empty(64);
        var committed = MatterFurnaceTransaction.smelt(
            MatterTier.RED, 3, new MatterFurnaceTransaction.Output(result, 2), List.of(empty), true
        );
        assertTrue(committed.committed());
        assertEquals(2, committed.resultingInputCount());
        assertEquals(4, committed.produced());
        assertEquals(Optional.of(result), committed.outputs().getFirst().variant());
        assertEquals(4, committed.outputs().getFirst().count());

        var full = new MatterFurnaceTransaction.OutputSlot(Optional.of(result), 63, 64);
        var rejected = MatterFurnaceTransaction.smelt(
            MatterTier.RED, 3, new MatterFurnaceTransaction.Output(result, 2), List.of(full), true
        );
        assertFalse(rejected.committed());
        assertEquals(3, rejected.resultingInputCount());
        assertEquals(full, rejected.outputs().getFirst());
    }

    @Test
    void fuelRemainderMustHaveAReservedSink() {
        CondenserVariant bucket = CondenserVariant.item("minecraft:bucket");
        var rejected = MatterFurnaceTransaction.ignite(1, 20_000, Optional.of(bucket), false);
        assertFalse(rejected.committed());
        assertEquals(1, rejected.resultingFuelCount());

        var committed = MatterFurnaceTransaction.ignite(1, 20_000, Optional.of(bucket), true);
        assertTrue(committed.committed());
        assertEquals(0, committed.resultingFuelCount());
        assertEquals(Optional.of(bucket), committed.craftingRemainder());
    }

    @Test
    void armorNeverGrantsInvulnerabilityAndPeriodicEffectsAreRateLimited() {
        var full = MatterArmorPolicy.evaluate(MatterTier.RED, 4, 20, false, 100, 80);
        assertEquals(0.90, full.reductionFraction(), 0.0001);
        assertEquals(2.0, full.resultingDamage(), 0.0001);
        assertTrue(full.periodicEffectAllowed());

        var throttled = MatterArmorPolicy.evaluate(MatterTier.RED, 4, 20, false, 100, 99);
        assertFalse(throttled.periodicEffectAllowed());
        var bypass = MatterArmorPolicy.evaluate(MatterTier.RED, 4, 20, true, 100, -1);
        assertEquals(20, bypass.resultingDamage(), 0.0001);
    }

    @Test
    void invalidAuditEventsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new MatterActionAuditEvent(
            UUID.randomUUID(), "red_matter", "mine", 1, 2, 0, EmcValue.ZERO, 0
        ));
    }
}
