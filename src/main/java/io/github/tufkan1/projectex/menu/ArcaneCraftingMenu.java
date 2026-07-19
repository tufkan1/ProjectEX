package io.github.tufkan1.projectex.menu;

import java.util.function.BooleanSupplier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;

/** Vanilla 3x3 crafting menu with item-identity authorization for portable use. */
public final class ArcaneCraftingMenu extends CraftingMenu {
    private final Player owner;
    private final BooleanSupplier authorization;

    public ArcaneCraftingMenu(int containerId, Inventory inventory, BooleanSupplier authorization) {
        super(containerId, inventory, ContainerLevelAccess.NULL);
        owner = inventory.player;
        this.authorization = java.util.Objects.requireNonNull(authorization, "authorization");
    }

    @Override public boolean stillValid(Player player) {
        return player == owner && player.isAlive() && authorization.getAsBoolean();
    }
}
