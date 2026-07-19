package io.github.tufkan1.projectex.compat.jade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.content.automation.AutomationBlockKind;
import io.github.tufkan1.projectex.content.component.AlchemyStorageState;
import io.github.tufkan1.projectex.machine.ExpansionMachineTier;
import io.github.tufkan1.projectex.machine.MachineAccess;
import io.github.tufkan1.projectex.machine.MachineRedstoneMode;
import io.github.tufkan1.projectex.machine.MachineState;
import io.github.tufkan1.projectex.machine.MachineTier;
import io.github.tufkan1.projectex.storage.StorageKind;
import io.github.tufkan1.projectex.storage.AdvancedStorageConfig;
import java.math.BigInteger;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JadeTooltipDataTest {
    @Test
    void deniedMachineAndStorageNeverExposePrivateBalances() {
        MachineTier tier = MachineTier.COLLECTOR_MK1;
        MachineState machine = new MachineState(1, tier, EmcValue.of(4096), BigInteger.ZERO,
            MachineAccess.ownedBy(UUID.fromString("00000000-0000-0000-0000-000000000001")),
            MachineRedstoneMode.IGNORED);

        var deniedMachine = JadeTooltipData.machine(tier, machine, false);
        assertFalse(deniedMachine.containsKey(ProjectEXMachineProvider.PREFIX + "stored"));
        assertFalse(deniedMachine.containsKey(ProjectEXMachineProvider.PREFIX + "capacity"));
        assertFalse(deniedMachine.containsKey(ProjectEXMachineProvider.PREFIX + "redstone"));
        assertTrue(deniedMachine.containsKey(ProjectEXMachineProvider.PREFIX + "type"));

        var deniedStorage = JadeTooltipData.storage(StorageKind.CONDENSER_MK3,
            new AlchemyStorageState(1, EmcValue.of(8192),
                MachineAccess.ownedBy(UUID.fromString("00000000-0000-0000-0000-000000000001")),
                AdvancedStorageConfig.DEFAULT), false);
        assertEquals(1, deniedStorage.size());
        assertFalse(deniedStorage.containsKey(ProjectEXStorageProvider.STORED));
    }

    @Test
    void permittedMachineGetsOnlyAllowlistedOperationalFields() {
        MachineTier tier = MachineTier.RELAY_MK1;
        MachineState machine = new MachineState(1, tier, EmcValue.of(256), BigInteger.ZERO,
            MachineAccess.ownedBy(UUID.fromString("00000000-0000-0000-0000-000000000001")),
            MachineRedstoneMode.REQUIRE_SIGNAL);

        var data = JadeTooltipData.machine(tier, machine, true);
        assertEquals("256", data.get(ProjectEXMachineProvider.PREFIX + "stored"));
        assertEquals(6, data.size());
        assertTrue(data.keySet().stream().noneMatch(key ->
            key.contains("owner") || key.contains("member") || key.contains("knowledge") || key.contains("filter")));
    }

    @Test
    void automationIdentityContainsNoAccountState() {
        var data = JadeTooltipData.automation(AutomationBlockKind.EMC_LINK, ExpansionMachineTier.FINAL);
        assertEquals(2, data.size());
        assertEquals("final", data.get(ProjectEXAutomationProvider.TIER));
    }
}
