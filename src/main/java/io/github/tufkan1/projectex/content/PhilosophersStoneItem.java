package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.content.alchemy.WorldTransmutationService;
import io.github.tufkan1.projectex.content.component.ActiveItemState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Server-authoritative active item for deterministic world transmutation. */
public final class PhilosophersStoneItem extends ChargeableUtilityItem {
    private static final int TRANSFORMATION_COOLDOWN_TICKS = 4;

    public PhilosophersStoneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        if (level.isClientSide()) {
            return WorldTransmutationService.hasTransformation(
                level.getBlockState(context.getClickedPos())
            ) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel serverLevel)
            || !(context.getPlayer() instanceof ServerPlayer player)
            || player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }

        ActiveItemState state = stack.getOrDefault(
            ProjectEXComponents.ACTIVE_ITEM_STATE,
            ActiveItemState.DEFAULT
        );
        WorldTransmutationService.Result result = WorldTransmutationService.transform(
            serverLevel,
            player,
            stack,
            context.getClickedPos(),
            context.getClickedFace(),
            context.getHorizontalDirection(),
            state,
            context.isSecondaryUseActive()
        );
        if (result.status() != WorldTransmutationService.Status.CHANGED) {
            return result.status() == WorldTransmutationService.Status.UNSUPPORTED
                ? InteractionResult.PASS
                : InteractionResult.FAIL;
        }

        player.getCooldowns().addCooldown(stack, TRANSFORMATION_COOLDOWN_TICKS);
        return InteractionResult.SUCCESS_SERVER;
    }

}
