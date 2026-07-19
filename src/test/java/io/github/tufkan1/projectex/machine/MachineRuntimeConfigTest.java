package io.github.tufkan1.projectex.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

final class MachineRuntimeConfigTest {
    @AfterEach
    void restoreDefaults() {
        System.clearProperty(MachineRuntimeConfig.COMPACT_SUN_MULTIPLIER_PROPERTY);
        MachineRuntimeConfig.reload();
    }

    @Test
    void compactSunMultiplierIsBoundedAndZeroDisablesBonus() {
        System.setProperty(MachineRuntimeConfig.COMPACT_SUN_MULTIPLIER_PROPERTY, "37");
        MachineRuntimeConfig.reload();
        assertEquals(37, MachineRuntimeConfig.compactSunMultiplier());

        System.setProperty(MachineRuntimeConfig.COMPACT_SUN_MULTIPLIER_PROPERTY, "0");
        MachineRuntimeConfig.reload();
        assertEquals(1, MachineRuntimeConfig.compactSunMultiplier());

        System.setProperty(MachineRuntimeConfig.COMPACT_SUN_MULTIPLIER_PROPERTY, "1000001");
        assertThrows(IllegalArgumentException.class, MachineRuntimeConfig::reload);
    }
}
