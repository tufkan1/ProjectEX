package io.github.tufkan1.projectex.emc.mapping.minecraft;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.display.SlotDisplay;

/** Recipe tag display bridge used through 26.3 Snapshot 3. */
final class RecipeTagCompat {
    private RecipeTagCompat() {
    }

    static List<Holder<Item>> items(SlotDisplay.TagSlotDisplay display) {
        List<Holder<Item>> items = new ArrayList<>();
        BuiltInRegistries.ITEM.getTagOrEmpty(display.tag()).forEach(items::add);
        return List.copyOf(items);
    }
}
