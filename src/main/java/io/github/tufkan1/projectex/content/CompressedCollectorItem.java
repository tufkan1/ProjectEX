package io.github.tufkan1.projectex.content;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Nine collectors compressed into a non-generating power-flower component. */
public final class CompressedCollectorItem extends Item {
    public CompressedCollectorItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
