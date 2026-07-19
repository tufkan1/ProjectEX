package io.github.tufkan1.projectex.emc.mapping.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcMatch;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MinecraftRecipeMappingServiceTest {
    @Test
    void removesOldDerivedValuesBeforeEveryRebuild() {
        EmcMatch explicit = EmcMatch.item(EmcKey.parse("minecraft:oak_log"));
        EmcMatch derived = EmcMatch.item(EmcKey.parse("minecraft:oak_planks"));

        MinecraftRecipeMappingService.ExplicitSnapshot snapshot = MinecraftRecipeMappingService.explicitValues(
            Map.of(explicit, EmcValue.of(32), derived, EmcValue.of(8)),
            Map.of(explicit, "data:projectex/base", derived, "recipe:minecraft:oak_planks")
        );

        assertEquals(Map.of(explicit, EmcValue.of(32)), snapshot.values());
        assertEquals(Map.of(explicit, "data:projectex/base"), snapshot.sources());
    }
}
