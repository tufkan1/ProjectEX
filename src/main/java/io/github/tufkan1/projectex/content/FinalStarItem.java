package io.github.tufkan1.projectex.content;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Endgame capability token; behavior is provided through the public Fabric lookup. */
public final class FinalStarItem extends Item {
    public FinalStarItem(Properties properties) { super(properties.stacksTo(1).fireResistant()); }
    @Override public boolean isFoil(ItemStack stack) { return true; }
}
