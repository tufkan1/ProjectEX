package io.github.tufkan1.projectex.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class CondenserTransactionTest {
    @Test
    void persistentCondenserCanAccumulateSubTargetInputsExactly() {
        CondenserVariant target = CondenserVariant.item("minecraft:diamond");
        var result = CondenserTransaction.evaluate(
            target,
            EmcValue.of(8_192),
            EmcValue.ZERO,
            List.of(new CondenserTransaction.Input(
                CondenserVariant.item("minecraft:coal"), EmcValue.of(128), 1
            )),
            64,
            1,
            true
        );
        assertEquals(EmcValue.of(128), result.stored());
        assertEquals(List.of(1), result.consumedCounts());
        assertEquals(0, result.produced());
        assertTrue(result.changed());
    }

    @Test
    void producesExactOutputsAndRetainsRemainder() {
        CondenserVariant target = CondenserVariant.item("minecraft:diamond");
        var result = CondenserTransaction.evaluate(
            target,
            EmcValue.of(8_192),
            EmcValue.of(1_000),
            List.of(new CondenserTransaction.Input(
                CondenserVariant.item("minecraft:gold_ingot"), EmcValue.of(2_048), 10
            )),
            2,
            10
        );
        assertEquals(2, result.produced());
        assertEquals(List.of(10), result.consumedCounts());
        assertEquals(EmcValue.of(5_096), result.stored());
    }

    @Test
    void componentVariantsCannotSubstituteForTheTarget() {
        CondenserVariant plain = CondenserVariant.item("minecraft:potion");
        CondenserVariant healing = new CondenserVariant("minecraft:potion", "{potion:healing}");
        CondenserVariant poison = new CondenserVariant("minecraft:potion", "{potion:poison}");
        var result = CondenserTransaction.evaluate(
            healing,
            EmcValue.of(100),
            EmcValue.of(100),
            List.of(
                new CondenserTransaction.Input(healing, EmcValue.of(100), 1),
                new CondenserTransaction.Input(poison, EmcValue.of(100), 1),
                new CondenserTransaction.Input(plain, EmcValue.of(100), 1)
            ),
            3,
            3
        );
        assertEquals(List.of(0, 1, 1), result.consumedCounts());
        assertEquals(3, result.produced());
        assertEquals(EmcValue.ZERO, result.stored());
    }

    @Test
    void fullOutputAndSubTargetInputDoNotConsumeAnything() {
        var full = CondenserTransaction.evaluate(
            CondenserVariant.item("minecraft:diamond"), EmcValue.of(8_192), EmcValue.of(1_000),
            List.of(new CondenserTransaction.Input(
                CondenserVariant.item("minecraft:gold_ingot"), EmcValue.of(2_048), 64
            )), 0, 64
        );
        assertFalse(full.changed());
        var insufficient = CondenserTransaction.evaluate(
            CondenserVariant.item("minecraft:diamond"), EmcValue.of(8_192), EmcValue.ZERO,
            List.of(new CondenserTransaction.Input(
                CondenserVariant.item("minecraft:gold_ingot"), EmcValue.of(2_048), 1
            )), 64, 1
        );
        assertFalse(insufficient.changed());
    }

    @Test
    void bagIdentityAndNestingPolicyRejectRecursiveContainers() {
        UUID owner = UUID.randomUUID();
        BagIdentity bag = BagIdentity.create("red", owner);
        BagNestingPolicy policy = new BagNestingPolicy(Set.of(
            "projectex:red_alchemical_bag", "projectex:blue_alchemical_bag"
        ));
        assertTrue(bag.permits(owner, false));
        assertFalse(bag.permits(UUID.randomUUID(), false));
        assertFalse(policy.canInsert(bag, "projectex:red_alchemical_bag", bag));
        assertFalse(policy.canInsert(bag, "projectex:blue_alchemical_bag", null));
        assertTrue(policy.canInsert(bag, "minecraft:diamond", null));
        assertEquals(Optional.of(owner), bag.owner());
    }
}
