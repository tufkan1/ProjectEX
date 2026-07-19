package io.github.tufkan1.projectex.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

final class ExpansionMachineTierTest {
    @Test
    void tierCatalogIsStableAndUsesSixfoldProgression() {
        assertEquals(16, ExpansionMachineTier.values().length);
        assertEquals("basic", ExpansionMachineTier.BASIC.id());
        assertEquals(1, ExpansionMachineTier.BASIC.level());
        assertEquals("final", ExpansionMachineTier.FINAL.id());
        assertEquals(16, ExpansionMachineTier.FINAL.level());

        for (int index = 1; index < ExpansionMachineTier.values().length; index++) {
            ExpansionMachineTier previous = ExpansionMachineTier.values()[index - 1];
            ExpansionMachineTier current = ExpansionMachineTier.values()[index];
            assertEquals(previous.collectorPerSecond().multiply(6), current.collectorPerSecond());
            assertEquals(previous.relayBonusPerSecond().multiply(6), current.relayBonusPerSecond());
        }
    }

    @Test
    void powerFlowerCompositionIsExact() {
        ExpansionMachineTier tier = ExpansionMachineTier.MAGENTA;
        EmcValue expected = tier.collectorPerSecond()
            .multiply(ExpansionMachineTier.POWER_FLOWER_COLLECTORS)
            .add(tier.relayBonusPerSecond().multiply(ExpansionMachineTier.POWER_FLOWER_RELAYS));
        assertEquals(expected, tier.powerFlowerPerSecond());
        assertEquals(BigInteger.valueOf(20), tier.powerFlowerRate().denominator());
    }

    @Test
    void fractionalTickGenerationNeverLosesEmc() {
        FixedPointRate rate = ExpansionMachineTier.BASIC.powerFlowerRate();
        FixedPointRate.Generation first = rate.generate(BigInteger.ZERO, 1, EmcValue.of(1_000));
        FixedPointRate.Generation rest = rate.generate(first.deferredNumerator(), 19, EmcValue.of(1_000));
        assertEquals(ExpansionMachineTier.BASIC.powerFlowerPerSecond(),
            first.produced().add(rest.produced()));
        assertEquals(BigInteger.ZERO, rest.deferredNumerator());
    }

    @Test
    void finalRelayKeepsUpstreamSaturationRuleWithoutOverflow() {
        assertEquals(new EmcValue(BigInteger.valueOf(Long.MAX_VALUE)),
            ExpansionMachineTier.FINAL.relayTransferPerTick());
        assertEquals(new EmcValue(BigInteger.valueOf(64).multiply(BigInteger.valueOf(6).pow(14))),
            ExpansionMachineTier.FADING.relayTransferPerTick());
    }

    @Test
    void machineCatalogMapsEveryPlayableExpansionBlockTier() {
        for (ExpansionMachineTier tier : ExpansionMachineTier.values()) {
            assertEquals(tier, MachineTier.expansion(MachineTier.MachineType.POWER_FLOWER, tier)
                .expansionTier().orElseThrow());
            if (tier.ordinal() >= ExpansionMachineTier.MAGENTA.ordinal()) {
                assertEquals(tier, MachineTier.expansion(MachineTier.MachineType.COLLECTOR, tier)
                    .expansionTier().orElseThrow());
                assertEquals(tier, MachineTier.expansion(MachineTier.MachineType.RELAY, tier)
                    .expansionTier().orElseThrow());
            }
        }
    }
}
