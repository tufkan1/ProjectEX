package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.matter.MatterArmorPolicy;
import io.github.tufkan1.projectex.matter.MatterTier;
import io.github.tufkan1.projectex.matter.MatterTierConfig;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;

/** Matter armor piece with one bounded server-only full-set maintenance effect. */
public final class MatterArmorItem extends Item {
    private static final List<EquipmentSlot> ARMOR_SLOTS = List.of(
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    );
    private final MatterTier tier;
    private final ArmorType type;

    public MatterArmorItem(Properties properties, MatterTier tier, ArmorType type) {
        super(properties.stacksTo(1));
        this.tier = tier;
        this.type = type;
    }

    public MatterTier tier() { return MatterTierConfig.resolve(tier); }
    public ArmorType armorType() { return type; }

    @Override public void inventoryTick(
        ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot
    ) {
        if (!(entity instanceof Player player) || type != ArmorType.CHESTPLATE
            || slot != EquipmentSlot.CHEST || level.getGameTime() % 20 != 0) return;
        int pieces = 0;
        MatterTier currentTier = tier();
        for (EquipmentSlot armorSlot : ARMOR_SLOTS) {
            if (player.getItemBySlot(armorSlot).getItem() instanceof MatterArmorItem armor
                && armor.tier().id().equals(currentTier.id())) pieces++;
        }
        var policy = MatterArmorPolicy.evaluate(currentTier, pieces, 0, false, level.getGameTime(), -1);
        if (!policy.periodicEffectAllowed()) return;
        player.setAirSupply(Math.min(player.getMaxAirSupply(), player.getAirSupply() + 20));
        player.clearFire();
        if (currentTier.id().equals(MatterTier.RED.id())
            && player.getHealth() < player.getMaxHealth()) player.heal(0.5F);
    }
}
