package io.github.tufkan1.projectex.api.matter;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Immutable per-position claim query issued before a matter tool action commits. */
public record MatterAreaActionContext(
    ServerLevel level,
    ServerPlayer player,
    ItemStack tool,
    String action,
    BlockPos position,
    BlockState originalState
) {
    public MatterAreaActionContext {
        java.util.Objects.requireNonNull(level, "level");
        java.util.Objects.requireNonNull(player, "player");
        tool = tool.copy();
        if (action == null || action.isBlank()) throw new IllegalArgumentException("Matter action is blank");
        java.util.Objects.requireNonNull(position, "position");
        java.util.Objects.requireNonNull(originalState, "originalState");
    }
}
