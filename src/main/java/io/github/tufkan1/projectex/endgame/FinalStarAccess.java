package io.github.tufkan1.projectex.endgame;

import io.github.tufkan1.projectex.api.endgame.FinalStarApi;
import io.github.tufkan1.projectex.api.endgame.FinalStarCapability;
import io.github.tufkan1.projectex.api.endgame.FinalStarContext;
import io.github.tufkan1.projectex.api.endgame.FinalStarSlot;
import io.github.tufkan1.projectex.content.ProjectEXItems;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Registers and discovers Final Star leases in explicitly allowed player slots. */
public final class FinalStarAccess {
    private FinalStarAccess() { }

    public static void register() {
        FinalStarApi.LOOKUP.registerForItems((stack, context) -> capability(stack, context),
            ProjectEXItems.FINAL_STAR.item());
    }

    public static Optional<FinalStarCapability> find(ServerPlayer player) {
        Optional<FinalStarCapability> main = find(player, player.getMainHandItem(), FinalStarSlot.MAIN_HAND);
        if (main.isPresent()) return main;
        Optional<FinalStarCapability> off = find(player, player.getOffhandItem(), FinalStarSlot.OFF_HAND);
        if (off.isPresent()) return off;
        var inventoryItems = player.getInventory().getNonEquipmentItems();
        for (int index = 0; index < inventoryItems.size(); index++) {
            if (index == player.getInventory().getSelectedSlot()) continue;
            ItemStack stack = inventoryItems.get(index);
            Optional<FinalStarCapability> inventory = find(player, stack, FinalStarSlot.INVENTORY);
            if (inventory.isPresent()) return inventory;
        }
        return Optional.empty();
    }

    private static Optional<FinalStarCapability> find(
        ServerPlayer player, ItemStack stack, FinalStarSlot slot
    ) {
        if (stack.isEmpty()) return Optional.empty();
        return FinalStarApi.find(stack, new FinalStarContext(player, slot));
    }

    private static FinalStarCapability capability(ItemStack stack, FinalStarContext context) {
        var config = EndgameRuntimeConfig.snapshot();
        if (!config.finalStarEnabled() || !config.finalStarSlots().contains(context.slot())) return null;
        return new FinalStarCapability() {
            @Override public FinalStarSlot slot() { return context.slot(); }
            @Override public int cooldownTicks() { return config.finalStarCooldownTicks(); }
            @Override public boolean ready() { return !context.player().getCooldowns().isOnCooldown(stack); }
            @Override public boolean activate() {
                if (!ready()) return false;
                context.player().getCooldowns().addCooldown(stack, cooldownTicks());
                return true;
            }
        };
    }
}
