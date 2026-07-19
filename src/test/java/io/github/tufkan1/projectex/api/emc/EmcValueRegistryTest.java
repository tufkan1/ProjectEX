package io.github.tufkan1.projectex.api.emc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

class EmcValueRegistryTest {
    @Test
    void replacesTheSnapshotAtomicallyAndExposesItAsImmutable() {
        EmcValueRegistry registry = new EmcValueRegistry();
        EmcKey coal = EmcKey.parse("minecraft:coal");
        registry.replaceAll(Map.of(EmcMatch.item(coal), EmcValue.of(128)));

        assertEquals(EmcValue.of(128), registry.find(coal).orElseThrow());
        assertEquals(1, registry.size());
        assertThrows(UnsupportedOperationException.class,
            () -> registry.snapshot().put(EmcMatch.item(EmcKey.parse("minecraft:diamond")), EmcValue.of(8192)));
    }
}
