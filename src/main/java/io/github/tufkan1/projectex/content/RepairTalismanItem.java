package io.github.tufkan1.projectex.content;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Once-per-second, non-stackable passive repair for a player's damaged inventory items. */
public final class RepairTalismanItem extends Item {
    public RepairTalismanItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof Player player) || level.getGameTime() % 20 != 0
            || firstTalisman(player) != stack) return;
        repairInventory(player);
    }

    public static int repairInventory(Player player) {
        int repaired = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack candidate = player.getInventory().getItem(slot);
            if (candidate.isDamaged()
                && (candidate != player.getMainHandItem() || !player.swinging)) {
                candidate.setDamageValue(candidate.getDamageValue() - 1);
                repaired++;
            }
        }
        return repaired;
    }

    private static ItemStack firstTalisman(Player player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack candidate = player.getInventory().getItem(slot);
            if (candidate.getItem() instanceof RepairTalismanItem) return candidate;
        }
        return ItemStack.EMPTY;
    }
}
