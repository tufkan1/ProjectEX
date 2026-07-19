package io.github.tufkan1.projectex.api.utility;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Immutable claim query issued before a utility observes or changes a world position. */
public record UtilityWorldActionContext(
    ServerLevel level,
    ServerPlayer player,
    ItemStack utility,
    String action,
    BlockPos position,
    BlockState state
) {
    public UtilityWorldActionContext {
        java.util.Objects.requireNonNull(level, "level");
        java.util.Objects.requireNonNull(player, "player");
        utility = utility.copy();
        if (action == null || action.isBlank()) throw new IllegalArgumentException("Utility action is blank");
        java.util.Objects.requireNonNull(position, "position");
        java.util.Objects.requireNonNull(state, "state");
    }
}
