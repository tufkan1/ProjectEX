package io.github.tufkan1.projectex.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.math.BigInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

final class MachineRuntimeConfigTest {
    @AfterEach
    void restoreDefaults() {
        System.clearProperty(MachineRuntimeConfig.COMPACT_SUN_MULTIPLIER_PROPERTY);
        System.clearProperty(MachineRuntimeConfig.COLLECTOR_RATE_MULTIPLIER_PROPERTY);
        System.clearProperty(MachineRuntimeConfig.RELAY_TRANSFER_MULTIPLIER_PROPERTY);
        System.clearProperty(MachineRuntimeConfig.POWER_FLOWER_RATE_MULTIPLIER_PROPERTY);
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

    @Test
    void rationalRateMultipliersRemainExactAndReloadAtomically() {
        System.setProperty(MachineRuntimeConfig.COLLECTOR_RATE_MULTIPLIER_PROPERTY, "3/2");
        System.setProperty(MachineRuntimeConfig.RELAY_TRANSFER_MULTIPLIER_PROPERTY, "0.5");
        System.setProperty(MachineRuntimeConfig.POWER_FLOWER_RATE_MULTIPLIER_PROPERTY, "7/4");
        MachineRuntimeConfig.reload();

        assertEquals(EmcValue.of(6), MachineRuntimeConfig.generationRate(MachineTier.COLLECTOR_MK1)
            .generate(BigInteger.ZERO, 1, EmcValue.of(100)).produced());
        assertEquals(EmcValue.of(32), MachineRuntimeConfig.transferLimit(MachineTier.RELAY_MK1));
        assertEquals(EmcValue.of(178), MachineRuntimeConfig
            .generationRate(MachineTier.POWER_FLOWER_BASIC)
            .generate(BigInteger.ZERO, 20, EmcValue.of(1_000)).produced());

        System.setProperty(MachineRuntimeConfig.POWER_FLOWER_RATE_MULTIPLIER_PROPERTY, "0");
        assertThrows(IllegalArgumentException.class, MachineRuntimeConfig::reload);
        assertEquals(EmcValue.of(6), MachineRuntimeConfig.generationRate(MachineTier.COLLECTOR_MK1)
            .generate(BigInteger.ZERO, 1, EmcValue.of(100)).produced());
    }

    @Test
    void multiplierParserCanonicalizesAndBoundsInput() {
        assertEquals(new MachineRateMultiplier(BigInteger.valueOf(3), BigInteger.valueOf(2)),
            MachineRateMultiplier.parse("1.500"));
        assertThrows(IllegalArgumentException.class, () -> MachineRateMultiplier.parse("1/0"));
        assertThrows(IllegalArgumentException.class, () -> MachineRateMultiplier.parse("0.0001"));
        assertThrows(IllegalArgumentException.class, () -> MachineRateMultiplier.parse("1000001"));
    }
}
