package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.menu.TransmutationMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Portable access point that reuses the exact server-owned M2 transmutation session. */
public final class TransmutationTabletItem extends Item {
    public TransmutationTabletItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        ItemStack tablet = player.getItemInHand(hand);
        serverPlayer.openMenu(new SimpleMenuProvider(
            (containerId, inventory, ignored) -> new TransmutationMenu(
                containerId, inventory, serverPlayer, null,
                () -> serverPlayer.getItemInHand(hand) == tablet
            ),
            Component.translatable("menu.projectex.transmutation_tablet")
        ));
        return InteractionResult.SUCCESS_SERVER;
    }
}
