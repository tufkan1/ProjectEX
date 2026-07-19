package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.menu.TransmutationMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Physical server-owned access point for player transmutation. */
public final class TransmutationTableBlock extends Block {
    public TransmutationTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        BlockHitResult hit
    ) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        serverPlayer.openMenu(new SimpleMenuProvider(
            (containerId, inventory, ignored) ->
                new TransmutationMenu(containerId, inventory, serverPlayer, pos),
            Component.translatable("menu.projectex.transmutation")
        ));
        return InteractionResult.SUCCESS_SERVER;
    }
}
