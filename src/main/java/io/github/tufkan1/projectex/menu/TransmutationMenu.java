package io.github.tufkan1.projectex.menu;

import io.github.tufkan1.projectex.content.ProjectEXMenus;
import io.github.tufkan1.projectex.content.ProjectEXBlocks;
import io.github.tufkan1.projectex.internal.player.MinecraftPlayerAlchemyTarget;
import io.github.tufkan1.projectex.network.AlchemyNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

/** Player-inventory transmutation menu; all alchemy actions still travel through validated payloads. */
public final class TransmutationMenu extends AbstractContainerMenu {
    private final ServerPlayer serverPlayer;
    private final ContainerLevelAccess access;
    private final java.util.function.BooleanSupplier portableAuthorization;

    public TransmutationMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, null, () -> true);
    }

    public TransmutationMenu(int containerId, Inventory inventory, ServerPlayer serverPlayer) {
        this(containerId, inventory, serverPlayer, null, () -> true);
    }

    public TransmutationMenu(
        int containerId,
        Inventory inventory,
        ServerPlayer serverPlayer,
        BlockPos tablePos
    ) {
        this(containerId, inventory, serverPlayer, tablePos, () -> true);
    }

    public TransmutationMenu(
        int containerId,
        Inventory inventory,
        ServerPlayer serverPlayer,
        BlockPos tablePos,
        java.util.function.BooleanSupplier portableAuthorization
    ) {
        super(ProjectEXMenus.TRANSMUTATION, containerId);
        this.serverPlayer = serverPlayer;
        this.portableAuthorization = portableAuthorization;
        this.access = serverPlayer != null && tablePos != null
            ? ContainerLevelAccess.create(serverPlayer.level(), tablePos)
            : ContainerLevelAccess.NULL;
        addStandardInventorySlots(inventory, 44, 150);
        if (serverPlayer != null) {
            MinecraftPlayerAlchemyTarget target = new MinecraftPlayerAlchemyTarget(serverPlayer);
            AlchemyNetworking.openSession(
                serverPlayer,
                target,
                () -> serverPlayer.containerMenu == this && portableAuthorization.getAsBoolean(),
                () -> tablePos == null ? 0 : serverPlayer.distanceToSqr(
                    tablePos.getX() + 0.5, tablePos.getY() + 0.5, tablePos.getZ() + 0.5)
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
            || (player == serverPlayer && serverPlayer.isAlive() && !serverPlayer.hasDisconnected()
                && portableAuthorization.getAsBoolean()
                && (access == ContainerLevelAccess.NULL
                    || stillValid(access, player, ProjectEXBlocks.TRANSMUTATION_TABLE)));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (serverPlayer != null) {
            AlchemyNetworking.closeSession(serverPlayer);
        }
    }
}
