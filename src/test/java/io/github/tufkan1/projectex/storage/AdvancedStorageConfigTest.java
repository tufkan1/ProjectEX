package io.github.tufkan1.projectex.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class AdvancedStorageConfigTest {
    @Test
    void filterModesAreDeterministicAndCaseNormalized() {
        AdvancedStorageConfig config = AdvancedStorageConfig.DEFAULT.toggle("Minecraft:Diamond");
        assertEquals(List.of("minecraft:diamond"), config.itemIds());
        assertTrue(config.allows("minecraft:coal"));

        config = config.cycleMode();
        assertTrue(config.allows("MINECRAFT:DIAMOND"));
        assertFalse(config.allows("minecraft:coal"));

        config = config.cycleMode();
        assertFalse(config.allows("minecraft:diamond"));
        assertTrue(config.allows("minecraft:coal"));
    }

    @Test
    void filterSetIsBoundedAndDuplicateFree() {
        AdvancedStorageConfig config = AdvancedStorageConfig.DEFAULT;
        for (int index = 0; index < AdvancedStorageConfig.MAX_FILTERS; index++) {
            config = config.toggle("test:item_" + index);
        }
        AdvancedStorageConfig full = config;
        assertEquals(AdvancedStorageConfig.MAX_FILTERS, full.itemIds().size());
        assertSame(full, full.toggle("test:overflow"));
        assertEquals(AdvancedStorageConfig.MAX_FILTERS - 1, full.toggle("test:item_0").itemIds().size());
    }
}
