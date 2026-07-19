package io.github.tufkan1.projectex.compat.jade;

import io.github.tufkan1.projectex.content.automation.AutomationBlockKind;
import io.github.tufkan1.projectex.content.component.AlchemyStorageState;
import io.github.tufkan1.projectex.machine.ExpansionMachineTier;
import io.github.tufkan1.projectex.machine.MachineState;
import io.github.tufkan1.projectex.machine.MachineTier;
import io.github.tufkan1.projectex.storage.StorageKind;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Pure allowlist builder: only returned fields are eligible for Jade synchronization. */
final class JadeTooltipData {
    static Map<String, String> machine(MachineTier tier, MachineState state, boolean authorized) {
        LinkedHashMap<String, String> data = new LinkedHashMap<>();
        data.put(ProjectEXMachineProvider.PREFIX + "tier", tier.name().toLowerCase(Locale.ROOT));
        data.put(ProjectEXMachineProvider.PREFIX + "type", tier.type().name().toLowerCase(Locale.ROOT));
        data.put(ProjectEXMachineProvider.PREFIX + "rate", tier.rate().amount().toString());
        if (authorized) {
            data.put(ProjectEXMachineProvider.PREFIX + "stored", state.stored().amount().toString());
            data.put(ProjectEXMachineProvider.PREFIX + "capacity", tier.capacity().amount().toString());
            data.put(ProjectEXMachineProvider.PREFIX + "redstone", state.redstoneMode().name().toLowerCase(Locale.ROOT));
        }
        return Collections.unmodifiableMap(data);
    }

    static Map<String, String> storage(StorageKind kind, AlchemyStorageState state, boolean authorized) {
        LinkedHashMap<String, String> data = new LinkedHashMap<>();
        data.put(ProjectEXStorageProvider.KIND, kind.name().toLowerCase(Locale.ROOT));
        if (kind.condenser() && authorized) {
            data.put(ProjectEXStorageProvider.STORED, state.stored().amount().toString());
        }
        return Collections.unmodifiableMap(data);
    }

    static Map<String, String> automation(AutomationBlockKind kind, ExpansionMachineTier tier) {
        return Map.of(
            ProjectEXAutomationProvider.KIND, kind.name().toLowerCase(Locale.ROOT),
            ProjectEXAutomationProvider.TIER, tier.id()
        );
    }

    private JadeTooltipData() {
    }
}
