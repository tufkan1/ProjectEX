package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.content.component.ActiveItemState;
import io.github.tufkan1.projectex.network.UtilityStateAction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Shared persistent charge/mode and accessible feedback contract for active utility items. */
public class ChargeableUtilityItem extends Item {
    public ChargeableUtilityItem(Properties properties) {
        super(properties.stacksTo(1).component(ProjectEXComponents.ACTIVE_ITEM_STATE,
            ActiveItemState.DEFAULT));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        cycle(player.getItemInHand(hand), player,
            player.isSecondaryUseActive() ? UtilityStateAction.MODE : UtilityStateAction.CHARGE);
        return InteractionResult.SUCCESS_SERVER;
    }

    public final void cycle(ItemStack stack, Player player, UtilityStateAction action) {
        if (stack.getItem() != this) return;
        ActiveItemState current = stack.getOrDefault(ProjectEXComponents.ACTIVE_ITEM_STATE,
            ActiveItemState.DEFAULT);
        ActiveItemState updated = action == UtilityStateAction.CHARGE
            ? current.nextCharge() : current.nextMode();
        stack.set(ProjectEXComponents.ACTIVE_ITEM_STATE, updated);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(Component.translatable(
                "item.projectex.utility.state",
                stack.getHoverName(),
                updated.charge(),
                Component.translatable("item.projectex.active_mode." + updated.mode().serializedName())
            ));
        }
    }
}
