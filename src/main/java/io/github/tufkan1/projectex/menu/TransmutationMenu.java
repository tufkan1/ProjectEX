package io.github.tufkan1.projectex.menu;

import io.github.tufkan1.projectex.content.ProjectEXMenus;
import io.github.tufkan1.projectex.internal.player.MinecraftPlayerAlchemyTarget;
import io.github.tufkan1.projectex.network.AlchemyNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** Player-inventory transmutation menu; all alchemy actions still travel through validated payloads. */
public final class TransmutationMenu extends AbstractContainerMenu {
    private final ServerPlayer serverPlayer;

    public TransmutationMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null);
    }

    public TransmutationMenu(int containerId, Inventory inventory, ServerPlayer serverPlayer) {
        super(ProjectEXMenus.TRANSMUTATION, containerId);
        this.serverPlayer = serverPlayer;
        addStandardInventorySlots(inventory, 44, 150);
        if (serverPlayer != null) {
            MinecraftPlayerAlchemyTarget target = new MinecraftPlayerAlchemyTarget(serverPlayer);
            AlchemyNetworking.openSession(
                serverPlayer,
                target,
                () -> serverPlayer.containerMenu == this,
                () -> 0
            );
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return serverPlayer == null
            || (player == serverPlayer && serverPlayer.isAlive() && !serverPlayer.hasDisconnected());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (serverPlayer != null) {
            AlchemyNetworking.closeSession(serverPlayer);
        }
    }
}
