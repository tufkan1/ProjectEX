package io.github.tufkan1.projectex.content;

import java.util.List;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/** First survival progression materials and active items. */
public final class ProjectEXItems {
    public static final ProjectEXContentRegistry.RegisteredItem<Item> LOW_COVALENCE_DUST =
        simple("low_covalence_dust");
    public static final ProjectEXContentRegistry.RegisteredItem<Item> MEDIUM_COVALENCE_DUST =
        simple("medium_covalence_dust");
    public static final ProjectEXContentRegistry.RegisteredItem<Item> HIGH_COVALENCE_DUST =
        simple("high_covalence_dust");
    public static final ProjectEXContentRegistry.RegisteredItem<Item> ALCHEMICAL_COAL =
        simple("alchemical_coal");
    public static final ProjectEXContentRegistry.RegisteredItem<Item> MOBIUS_FUEL =
        simple("mobius_fuel");
    public static final ProjectEXContentRegistry.RegisteredItem<Item> AETERNALIS_FUEL =
        ProjectEXContentRegistry.registerItem(
            "aeternalis_fuel",
            Item::new,
            new Item.Properties().rarity(Rarity.UNCOMMON)
        );
    public static final ProjectEXContentRegistry.RegisteredItem<Item> DARK_MATTER =
        ProjectEXContentRegistry.registerItem(
            "dark_matter",
            Item::new,
            new Item.Properties().rarity(Rarity.RARE).fireResistant()
        );
    public static final ProjectEXContentRegistry.RegisteredItem<Item> RED_MATTER =
        ProjectEXContentRegistry.registerItem(
            "red_matter",
            Item::new,
            new Item.Properties().rarity(Rarity.EPIC).fireResistant()
        );
    public static final ProjectEXContentRegistry.RegisteredItem<PhilosophersStoneItem> PHILOSOPHERS_STONE =
        ProjectEXContentRegistry.registerItem(
            "philosophers_stone",
            PhilosophersStoneItem::new,
            new Item.Properties().rarity(Rarity.UNCOMMON)
        );

    private static final List<ProjectEXContentRegistry.RegisteredItem<? extends Item>> MATERIALS = List.of(
        LOW_COVALENCE_DUST,
        MEDIUM_COVALENCE_DUST,
        HIGH_COVALENCE_DUST,
        ALCHEMICAL_COAL,
        MOBIUS_FUEL,
        AETERNALIS_FUEL,
        DARK_MATTER,
        RED_MATTER
    );

    private ProjectEXItems() {
    }

    public static void register() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
            .register(entries -> MATERIALS.forEach(entry -> entries.accept(entry.item())));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
            .register(entries -> entries.accept(PHILOSOPHERS_STONE.item()));
    }

    public static List<ProjectEXContentRegistry.RegisteredItem<? extends Item>> materials() {
        return MATERIALS;
    }

    private static ProjectEXContentRegistry.RegisteredItem<Item> simple(String path) {
        return ProjectEXContentRegistry.registerItem(path, Item::new, new Item.Properties());
    }
}
