package io.github.tufkan1.projectex.content;

import java.util.List;
import net.fabricmc.fabric.api.registry.FuelValueEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Fuel registry bridge used through 26.3 Snapshot 3. */
final class FuelCompatLegacy {
    private FuelCompatLegacy() {
    }

    static void register(Item alchemical, Item mobius, Item aeternalis, List<Item> expansion) {
        FuelValueEvents.BUILD.register((builder, context) -> {
            builder.add(alchemical, 1_600);
            builder.add(mobius, 6_400);
            builder.add(aeternalis, 25_600);
            expansion.forEach(item -> builder.add(item, 25_600));
        });
    }

    public static int burnDuration(ServerLevel level, ItemStack stack, Container container) {
        return level.fuelValues().burnDuration(stack);
    }

    public static boolean isFuel(ServerLevel level, ItemStack stack, Container container) {
        return level.fuelValues().isFuel(stack);
    }
}
