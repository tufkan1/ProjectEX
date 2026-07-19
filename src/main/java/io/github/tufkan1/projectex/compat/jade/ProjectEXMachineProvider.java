package io.github.tufkan1.projectex.compat.jade;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.content.machine.EmcMachineBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Server-authoritative machine summary with physically separated common and client providers. */
public final class ProjectEXMachineProvider {
    static final Identifier UID = ProjectEX.id("jade_machine");
    static final String PREFIX = "projectex_machine_";

    public enum Server implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof EmcMachineBlockEntity machine)) return;
            JadeTooltipData.machine(machine.tier(), machine.machineState(), machine.canUse(accessor.getPlayer()))
                .forEach(data::putString);
        }

        @Override public Identifier getUid() { return UID; }
    }

    public enum Client implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            data.getString(PREFIX + "type").ifPresent(type -> tooltip.add(Component.translatable(
                "compat.projectex.jade.machine", Component.translatable("compat.projectex.jade.machine." + type),
                data.getString(PREFIX + "tier").orElse("?"), data.getString(PREFIX + "rate").orElse("0"))));
            data.getString(PREFIX + "stored").ifPresent(stored -> tooltip.add(Component.translatable(
                "compat.projectex.jade.stored", stored, data.getString(PREFIX + "capacity").orElse("?"))));
            data.getString(PREFIX + "redstone").ifPresent(mode -> tooltip.add(Component.translatable(
                "compat.projectex.jade.redstone", Component.translatable("compat.projectex.jade.redstone." + mode))));
        }

        @Override public Identifier getUid() { return UID; }
    }

    private ProjectEXMachineProvider() {
    }
}
