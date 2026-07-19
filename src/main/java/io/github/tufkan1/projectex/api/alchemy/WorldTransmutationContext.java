package io.github.tufkan1.projectex.api.alchemy;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Immutable claim/protection query issued before a world transmutation commits. */
public record WorldTransmutationContext(
    ServerLevel level,
    ServerPlayer player,
    ItemStack catalyst,
    BlockPos position,
    BlockState originalState,
    BlockState targetState
) {
}
