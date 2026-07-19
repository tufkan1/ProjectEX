package io.github.tufkan1.projectex.content.pedestal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Server-only bounded effect contract for items accepted by a Dark Matter Pedestal. */
public interface PedestalEffectItem {
    int pedestalIntervalTicks();

    void applyPedestalEffect(ServerLevel level, BlockPos pedestalPos, ItemStack stack,
                             int range, int maximumPlayers);
}
