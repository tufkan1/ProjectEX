package io.github.tufkan1.projectex.content;

import java.util.List;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.DyeColor;

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
    public static final ProjectEXContentRegistry.RegisteredItem<KleinStarItem> KLEIN_STAR_EIN =
        star(KleinStarTier.EIN);
    public static final ProjectEXContentRegistry.RegisteredItem<KleinStarItem> KLEIN_STAR_ZWEI =
        star(KleinStarTier.ZWEI);
    public static final ProjectEXContentRegistry.RegisteredItem<KleinStarItem> KLEIN_STAR_DREI =
        star(KleinStarTier.DREI);
    public static final ProjectEXContentRegistry.RegisteredItem<KleinStarItem> KLEIN_STAR_VIER =
        star(KleinStarTier.VIER);
    public static final ProjectEXContentRegistry.RegisteredItem<KleinStarItem> KLEIN_STAR_SPHERE =
        star(KleinStarTier.SPHERE);
    public static final ProjectEXContentRegistry.RegisteredItem<KleinStarItem> KLEIN_STAR_OMEGA =
        star(KleinStarTier.OMEGA);
    public static final List<ProjectEXContentRegistry.RegisteredItem<AlchemicalBagItem>> ALCHEMICAL_BAGS =
        java.util.Arrays.stream(DyeColor.values()).map(ProjectEXItems::bag).toList();

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
    private static final List<ProjectEXContentRegistry.RegisteredItem<KleinStarItem>> KLEIN_STARS =
        List.of(
            KLEIN_STAR_EIN,
            KLEIN_STAR_ZWEI,
            KLEIN_STAR_DREI,
            KLEIN_STAR_VIER,
            KLEIN_STAR_SPHERE,
            KLEIN_STAR_OMEGA
        );

    private ProjectEXItems() {
    }

    public static void register() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
            .register(entries -> MATERIALS.forEach(entry -> entries.accept(entry.item())));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
            .register(entries -> {
                entries.accept(PHILOSOPHERS_STONE.item());
                KLEIN_STARS.forEach(entry -> entries.accept(entry.item()));
                ALCHEMICAL_BAGS.forEach(entry -> entries.accept(entry.item()));
            });
    }

    public static List<ProjectEXContentRegistry.RegisteredItem<? extends Item>> materials() {
        return MATERIALS;
    }

    public static List<ProjectEXContentRegistry.RegisteredItem<KleinStarItem>> kleinStars() {
        return KLEIN_STARS;
    }

    public static List<ProjectEXContentRegistry.RegisteredItem<AlchemicalBagItem>> alchemicalBags() {
        return ALCHEMICAL_BAGS;
    }

    private static ProjectEXContentRegistry.RegisteredItem<Item> simple(String path) {
        return ProjectEXContentRegistry.registerItem(path, Item::new, new Item.Properties());
    }

    private static ProjectEXContentRegistry.RegisteredItem<KleinStarItem> star(KleinStarTier tier) {
        return ProjectEXContentRegistry.registerItem(
            "klein_star_" + tier.serializedName(),
            properties -> new KleinStarItem(properties, tier),
            new Item.Properties().rarity(Rarity.UNCOMMON)
        );
    }

    private static ProjectEXContentRegistry.RegisteredItem<AlchemicalBagItem> bag(DyeColor color) {
        return ProjectEXContentRegistry.registerItem(
            color.getName() + "_alchemical_bag",
            properties -> new AlchemicalBagItem(properties, color),
            new Item.Properties().rarity(Rarity.UNCOMMON)
        );
    }
}
