package io.github.tufkan1.projectex.internal.teleport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import io.github.tufkan1.projectex.teleport.AlchemicalBookLocations;
import io.github.tufkan1.projectex.teleport.AlchemicalDestination;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class AlchemicalBookSavedDataTest {
    @Test void boundLocationsPersistAndCompareAndSetRejectsStaleRevision() {
        UUID owner = UUID.randomUUID();
        AlchemicalBookSavedData data = new AlchemicalBookSavedData();
        AlchemicalBookLocations before = data.locations(owner);
        AlchemicalBookLocations after = before.add(
            new AlchemicalDestination("Home", "minecraft:overworld", 1, 64, 2));
        assertTrue(data.compareAndSet(owner, before, 0, after));
        assertFalse(data.compareAndSet(owner, before, 0, AlchemicalBookLocations.EMPTY));
        var encoded = AlchemicalBookSavedData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
        AlchemicalBookSavedData restored = AlchemicalBookSavedData.CODEC
            .parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(after, restored.locations(owner));
    }
}
