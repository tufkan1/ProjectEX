package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.content.component.ArcaneTabletState;
import io.github.tufkan1.projectex.menu.ArcaneCraftingMenu;
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

/** Portable transmutation/crafting modes bound to the exact opening stack identity. */
public final class ArcaneTabletItem extends Item {
    public ArcaneTabletItem(Properties properties) {
        super(properties.stacksTo(1).component(ProjectEXComponents.ARCANE_TABLET_STATE,
            ArcaneTabletState.DEFAULT));
    }

    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        ItemStack tablet = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            ArcaneTabletState updated = tablet.getOrDefault(
                ProjectEXComponents.ARCANE_TABLET_STATE, ArcaneTabletState.DEFAULT).next();
            tablet.set(ProjectEXComponents.ARCANE_TABLET_STATE, updated);
            serverPlayer.sendOverlayMessage(Component.translatable(
                "item.projectex.arcane_tablet.mode",
                Component.translatable("item.projectex.arcane_tablet.mode."
                    + updated.mode().name().toLowerCase(java.util.Locale.ROOT))
            ));
            return InteractionResult.SUCCESS_SERVER;
        }
        open(serverPlayer, tablet, () -> serverPlayer.getItemInHand(hand) == tablet
            && !serverPlayer.hasDisconnected());
        return InteractionResult.SUCCESS_SERVER;
    }

    public static boolean openFromInventory(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack tablet = player.getInventory().getItem(slot);
            if (tablet.getItem() instanceof ArcaneTabletItem) {
                int lockedSlot = slot;
                open(player, tablet, () -> player.getInventory().getItem(lockedSlot) == tablet
                    && !player.hasDisconnected());
                return true;
            }
        }
        return false;
    }

    private static void open(ServerPlayer serverPlayer, ItemStack tablet,
                             java.util.function.BooleanSupplier authorized) {
        ArcaneTabletState state = tablet.getOrDefault(
            ProjectEXComponents.ARCANE_TABLET_STATE, ArcaneTabletState.DEFAULT);
        if (state.mode() == ArcaneTabletState.Mode.CRAFTING) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                (id, inventory, ignored) -> new ArcaneCraftingMenu(id, inventory, authorized),
                Component.translatable("menu.projectex.arcane_tablet.crafting")
            ));
        } else {
            serverPlayer.openMenu(new SimpleMenuProvider(
                (id, inventory, ignored) -> new TransmutationMenu(id, inventory, serverPlayer, null, authorized),
                Component.translatable("menu.projectex.arcane_tablet.transmutation")
            ));
        }
    }
}
