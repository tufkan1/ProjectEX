package io.github.tufkan1.projectex.emc.mapping.minecraft;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.display.SlotDisplay;

/** Holder-set recipe tag display bridge introduced by 26.3 Snapshot 4. */
final class RecipeTagCompat {
    private RecipeTagCompat() {
    }

    static List<Holder<Item>> items(SlotDisplay.TagSlotDisplay display) {
        return display.tag().stream().toList();
    }
}
