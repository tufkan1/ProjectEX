package io.github.tufkan1.projectex.teleport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

final class AlchemicalBookCoreTest {
    private static final AlchemicalDestination HOME = new AlchemicalDestination(
        "Home", "minecraft:overworld", 3, 64, 4);

    @Test void namesAreUniqueCaseInsensitiveAndBounded() {
        AlchemicalBookLocations locations = AlchemicalBookLocations.EMPTY.add(HOME);
        assertEquals(HOME, locations.find(" home ").orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> locations.add(
            new AlchemicalDestination("HOME", "minecraft:overworld", 0, 64, 0)));
        assertThrows(IllegalArgumentException.class, () -> new AlchemicalDestination(
            "@back", "minecraft:overworld", 0, 64, 0));
    }

    @Test void backTargetIsSeparateAndOneShotFriendly() {
        AlchemicalBookLocations locations = new AlchemicalBookLocations(List.of(HOME), Optional.empty())
            .withBack(new AlchemicalDestination("Previous", "minecraft:overworld", 0, 64, 0));
        assertTrue(locations.back().isPresent());
        assertFalse(locations.clearBack().back().isPresent());
        assertEquals(List.of(HOME), locations.clearBack().destinations());
    }

    @Test void tierCapabilitiesAndDistancePricesAreExact() {
        assertEquals(EmcValue.of(5_000), AlchemicalBookTier.BASIC.cost(new BlockPos(0, 64, 0), HOME, false));
        assertEquals(EmcValue.of(2_500), AlchemicalBookTier.ADVANCED.cost(new BlockPos(0, 64, 0), HOME, false));
        assertEquals(EmcValue.ZERO, AlchemicalBookTier.ARCANE.cost(new BlockPos(0, 64, 0), HOME, false));
        assertFalse(AlchemicalBookTier.ADVANCED.crossDimension());
        assertTrue(AlchemicalBookTier.MASTER.crossDimension());
    }
}
