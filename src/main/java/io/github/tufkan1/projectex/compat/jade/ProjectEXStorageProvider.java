package io.github.tufkan1.projectex.compat.jade;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.content.storage.AlchemyStorageBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Condenser summary that omits inventories, targets, owners, filters, and denied balances. */
public final class ProjectEXStorageProvider {
    static final Identifier UID = ProjectEX.id("jade_storage");
    static final String KIND = "projectex_storage_kind";
    static final String STORED = "projectex_storage_emc";

    public enum Server implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof AlchemyStorageBlockEntity storage)) return;
            JadeTooltipData.storage(storage.kind(), storage.storageState(), storage.canUse(accessor.getPlayer()))
                .forEach(data::putString);
        }

        @Override public Identifier getUid() { return UID; }
    }

    public enum Client implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            accessor.getServerData().getString(STORED).ifPresent(stored ->
                tooltip.add(Component.translatable("compat.projectex.jade.condenser_stored", stored)));
        }

        @Override public Identifier getUid() { return UID; }
    }

    private ProjectEXStorageProvider() {
    }
}
