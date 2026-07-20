package io.github.tufkan1.projectex.content;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Stable facade for the event-based and component-based cooking fuel systems. */
public final class FuelCompat {
    private FuelCompat() {
    }

    static void register(Item alchemical, Item mobius, Item aeternalis, List<Item> blocks, List<Item> expansion) {
        FuelCompatLegacy.register(alchemical, mobius, aeternalis, blocks, expansion);
    }

    public static int burnDuration(ServerLevel level, ItemStack stack, Container container) {
        return FuelCompatLegacy.burnDuration(level, stack, container);
    }

    public static boolean isFuel(ServerLevel level, ItemStack stack, Container container) {
        return FuelCompatLegacy.isFuel(level, stack, container);
    }
}
