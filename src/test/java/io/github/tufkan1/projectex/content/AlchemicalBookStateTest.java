package io.github.tufkan1.projectex.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mojang.serialization.JsonOps;
import io.github.tufkan1.projectex.content.component.AlchemicalBookState;
import io.github.tufkan1.projectex.teleport.AlchemicalBookLocations;
import io.github.tufkan1.projectex.teleport.AlchemicalDestination;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class AlchemicalBookStateTest {
    @Test void codecRoundTripsOwnerStackDestinationsAndBackTarget() {
        AlchemicalBookLocations locations = AlchemicalBookLocations.EMPTY
            .add(new AlchemicalDestination("Home", "minecraft:overworld", 1, 64, 2))
            .withBack(new AlchemicalDestination("Previous", "minecraft:the_nether", 3, 70, 4));
        AlchemicalBookState state = new AlchemicalBookState(
            AlchemicalBookState.CURRENT_VERSION, Optional.of(UUID.randomUUID()), locations);
        var encoded = AlchemicalBookState.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow();
        assertEquals(state, AlchemicalBookState.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
    }

    @Test void unsupportedLegacyVersionFailsClosedWhileMissingComponentUsesEmptyDefault() {
        assertEquals(Optional.empty(), AlchemicalBookState.EMPTY.owner());
        assertEquals(AlchemicalBookLocations.EMPTY, AlchemicalBookState.EMPTY.stackLocations());
        String legacy = """
            {"version":0,"stack_locations":{"destinations":[]}}
            """;
        assertThrows(IllegalStateException.class, () -> AlchemicalBookState.CODEC.parse(
            JsonOps.INSTANCE, com.google.gson.JsonParser.parseString(legacy)).getOrThrow());
    }
}
