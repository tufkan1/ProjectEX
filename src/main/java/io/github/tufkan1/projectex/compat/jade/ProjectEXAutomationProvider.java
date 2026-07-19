package io.github.tufkan1.projectex.compat.jade;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.content.automation.AutomationBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Public automation identity only; account, knowledge, owner, members, and filters stay server-side. */
public final class ProjectEXAutomationProvider {
    static final Identifier UID = ProjectEX.id("jade_automation");
    static final String KIND = "projectex_automation_kind";
    static final String TIER = "projectex_automation_tier";

    public enum Server implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof AutomationBlockEntity automation)) return;
            JadeTooltipData.automation(automation.kind(), automation.tier()).forEach(data::putString);
        }

        @Override public Identifier getUid() { return UID; }
    }

    public enum Client implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            accessor.getServerData().getString(KIND).ifPresent(kind -> tooltip.add(Component.translatable(
                "compat.projectex.jade.automation", Component.translatable("compat.projectex.jade.automation." + kind),
                accessor.getServerData().getString(TIER).orElse("?"))));
        }

        @Override public Identifier getUid() { return UID; }
    }

    private ProjectEXAutomationProvider() {
    }
}
