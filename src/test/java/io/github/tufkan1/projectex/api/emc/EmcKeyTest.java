package io.github.tufkan1.projectex.api.emc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EmcKeyTest {
    @Test
    void parsesNamespacedKeys() {
        EmcKey key = EmcKey.parse("minecraft:diamond_block");
        assertEquals("minecraft", key.namespace());
        assertEquals("diamond_block", key.path());
    }

    @Test
    void rejectsMalformedKeys() {
        assertThrows(IllegalArgumentException.class, () -> EmcKey.parse("diamond"));
        assertThrows(IllegalArgumentException.class, () -> EmcKey.parse("Minecraft:diamond"));
        assertThrows(IllegalArgumentException.class, () -> EmcKey.parse("minecraft:"));
    }
}
