package io.github.tufkan1.projectex.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class MachineCoreTest {
    @Test
    void fixedPointGenerationDefersBudgetedValueExactly() {
        FixedPointRate rate = new FixedPointRate(BigInteger.valueOf(5), BigInteger.valueOf(2));
        FixedPointRate.Generation first = rate.generate(BigInteger.ZERO, 3, EmcValue.of(4));
        assertEquals(EmcValue.of(4), first.produced());
        assertEquals(BigInteger.valueOf(7), first.deferredNumerator());

        FixedPointRate.Generation second = rate.generate(
            first.deferredNumerator(),
            1,
            EmcValue.of(20)
        );
        assertEquals(EmcValue.of(6), second.produced());
        assertEquals(BigInteger.ZERO, second.deferredNumerator());
    }

    @Test
    void networkRejectsCyclesAndConservesEmc() {
        MachineBuffer a = new MachineBuffer(EmcValue.of(1_000), EmcValue.of(300));
        MachineBuffer b = new MachineBuffer(EmcValue.of(1_000), EmcValue.of(200));
        MachineBuffer c = new MachineBuffer(EmcValue.of(1_000), EmcValue.ZERO);
        MachineNetworkTick tick = new MachineNetworkTick(
            new MachineTickBudget(10, EmcValue.of(1_000))
        );

        assertEquals(EmcValue.of(100), tick.route("a", a, "b", b, EmcValue.of(100)).moved());
        assertEquals(EmcValue.of(75), tick.route("b", b, "c", c, EmcValue.of(75)).moved());
        MachineNetworkTick.Transfer cycle = tick.route("c", c, "a", a, EmcValue.of(50));

        assertFalse(cycle.allowed());
        assertEquals(EmcValue.ZERO, cycle.moved());
        assertEquals(EmcValue.of(500), a.stored().add(b.stored()).add(c.stored()));
    }

    @Test
    void networkHonorsWorkAndValueBudgets() {
        MachineBuffer source = new MachineBuffer(EmcValue.of(1_000), EmcValue.of(1_000));
        MachineBuffer one = new MachineBuffer(EmcValue.of(1_000), EmcValue.ZERO);
        MachineBuffer two = new MachineBuffer(EmcValue.of(1_000), EmcValue.ZERO);
        MachineNetworkTick tick = new MachineNetworkTick(
            new MachineTickBudget(1, EmcValue.of(80))
        );

        assertEquals(EmcValue.of(80), tick.route("source", source, "one", one, EmcValue.of(100)).moved());
        assertFalse(tick.route("source", source, "two", two, EmcValue.of(100)).allowed());
        assertEquals(EmcValue.of(1_000), source.stored().add(one.stored()).add(two.stored()));
    }

    @Test
    void stateCodecRoundTripsAndRejectsCorruption() {
        UUID owner = UUID.randomUUID();
        MachineState state = new MachineState(
            MachineState.CURRENT_VERSION,
            MachineTier.COLLECTOR_MK2,
            EmcValue.of(12_345),
            BigInteger.valueOf(7),
            new MachineAccess(java.util.Optional.of(owner), true),
            MachineRedstoneMode.REQUIRE_SIGNAL
        );
        assertEquals(state, MachineStateCodec.decode(
            MachineStateCodec.encode(state),
            MachineTier.COLLECTOR_MK2
        ));

        Map<String, String> overflow = new HashMap<>(MachineStateCodec.encode(state));
        overflow.put("stored", "30001");
        assertThrows(IllegalArgumentException.class, () -> MachineStateCodec.decode(
            overflow,
            MachineTier.COLLECTOR_MK2
        ));
        Map<String, String> negative = new HashMap<>(MachineStateCodec.encode(state));
        negative.put("stored", "-1");
        assertThrows(IllegalArgumentException.class, () -> MachineStateCodec.decode(
            negative,
            MachineTier.COLLECTOR_MK2
        ));
    }

    @Test
    void accessAndRedstonePoliciesAreExplicit() {
        UUID owner = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        MachineAccess access = MachineAccess.ownedBy(owner);
        assertTrue(access.permits(owner, false));
        assertFalse(access.permits(stranger, false));
        assertTrue(access.permits(stranger, true));
        assertThrows(SecurityException.class, () -> access.withPublicAccess(true, stranger, false));
        assertTrue(access.withPublicAccess(true, owner, false).permits(stranger, false));
        assertTrue(MachineRedstoneMode.REQUIRE_SIGNAL.enabled(true));
        assertFalse(MachineRedstoneMode.REQUIRE_NO_SIGNAL.enabled(true));
    }

    @Test
    void fuelUpgradeSpendsOnlyTheExactDifference() {
        MachineBuffer buffer = new MachineBuffer(EmcValue.of(10_000), EmcValue.of(9_000));
        FuelUpgradeRule rule = new FuelUpgradeRule(
            "projectex:alchemical_coal",
            "projectex:mobius_fuel",
            EmcValue.of(512),
            EmcValue.of(2_048)
        );
        FuelUpgradeRule.Upgrade result = rule.apply("projectex:alchemical_coal", buffer);
        assertTrue(result.upgraded());
        assertEquals("projectex:mobius_fuel", result.resultId());
        assertEquals(EmcValue.of(1_536), result.spent());
        assertEquals(EmcValue.of(7_464), buffer.stored());
    }
}
