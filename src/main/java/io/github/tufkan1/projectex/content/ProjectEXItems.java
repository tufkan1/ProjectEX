package io.github.tufkan1.projectex.content;

import java.util.List;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.DyeColor;
import io.github.tufkan1.projectex.matter.MatterTier;
import java.util.Map;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.fabricmc.fabric.api.registry.FuelValueEvents;

/** First survival progression materials and active items. */
public final class ProjectEXItems {
    private static final ToolMaterial DARK_TOOL_MATERIAL = new ToolMaterial(
        ProjectEXTags.INCORRECT_FOR_DARK_MATTER_TOOL, 4_096, 14.0F, 3.0F, 1,
        ProjectEXTags.DARK_MATTER_REPAIR
    );
    private static final ToolMaterial RED_TOOL_MATERIAL = new ToolMaterial(
        ProjectEXTags.INCORRECT_FOR_RED_MATTER_TOOL, 8_192, 16.0F, 4.0F, 1,
        ProjectEXTags.RED_MATTER_REPAIR
    );
    private static final ArmorMaterial DARK_ARMOR_MATERIAL = armorMaterial(
        48, 3, 8, 6, 3, 3.0F, 0.1F, ProjectEXTags.DARK_MATTER_REPAIR
    );
    private static final ArmorMaterial RED_ARMOR_MATERIAL = armorMaterial(
        64, 4, 9, 7, 4, 4.0F, 0.2F, ProjectEXTags.RED_MATTER_REPAIR
    );
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
    public static final List<ProjectEXContentRegistry.RegisteredItem<Item>> EXPANSION_FUELS =
        java.util.Arrays.stream(ExpansionMaterialTier.values()).map(tier -> material(
            tier.fuelId(), tier.ordinal() < 4 ? Rarity.UNCOMMON
                : tier.ordinal() < 9 ? Rarity.RARE : Rarity.EPIC
        )).toList();
    public static final List<ProjectEXContentRegistry.RegisteredItem<Item>> EXPANSION_MATTERS =
        java.util.Arrays.stream(ExpansionMaterialTier.values()).map(tier -> material(
            tier.matterId(), tier.ordinal() < 4 ? Rarity.UNCOMMON
                : tier.ordinal() < 9 ? Rarity.RARE : Rarity.EPIC
        )).toList();
    public static final ProjectEXContentRegistry.RegisteredItem<Item> FADING_MATTER =
        material("fading_matter", Rarity.EPIC);
    public static final List<ProjectEXContentRegistry.RegisteredItem<CompressedCollectorItem>>
        COMPRESSED_COLLECTORS = java.util.Arrays.stream(
            io.github.tufkan1.projectex.machine.ExpansionMachineTier.values()
        ).map(tier -> ProjectEXContentRegistry.registerItem(
            tier.id() + "_compressed_collector",
            CompressedCollectorItem::new,
            new Item.Properties().rarity(tier.ordinal() < 4 ? Rarity.UNCOMMON
                : tier.ordinal() < 12 ? Rarity.RARE : Rarity.EPIC).fireResistant()
        )).toList();
    public static final ProjectEXContentRegistry.RegisteredItem<Item> FINAL_STAR_SHARD =
        material("final_star_shard", Rarity.EPIC);
    public static final ProjectEXContentRegistry.RegisteredItem<FinalStarItem> FINAL_STAR =
        ProjectEXContentRegistry.registerItem(
            "final_star", FinalStarItem::new, new Item.Properties().rarity(Rarity.EPIC)
        );
    public static final ProjectEXContentRegistry.RegisteredItem<InfiniteSteakItem> INFINITE_STEAK =
        ProjectEXContentRegistry.registerItem(
            "infinite_steak", InfiniteSteakItem::new, new Item.Properties().rarity(Rarity.RARE)
        );
    public static final ProjectEXContentRegistry.RegisteredItem<PhilosophersStoneItem> PHILOSOPHERS_STONE =
        ProjectEXContentRegistry.registerItem(
            "philosophers_stone",
            PhilosophersStoneItem::new,
            new Item.Properties().rarity(Rarity.UNCOMMON)
        );
    public static final ProjectEXContentRegistry.RegisteredItem<TransmutationTabletItem>
        TRANSMUTATION_TABLET = ProjectEXContentRegistry.registerItem(
            "transmutation_tablet", TransmutationTabletItem::new,
            new Item.Properties().rarity(Rarity.EPIC).fireResistant()
        );
    public static final ProjectEXContentRegistry.RegisteredItem<RepairTalismanItem> REPAIR_TALISMAN =
        ProjectEXContentRegistry.registerItem(
            "repair_talisman", RepairTalismanItem::new,
            new Item.Properties().rarity(Rarity.UNCOMMON)
        );
    public static final ProjectEXContentRegistry.RegisteredItem<ElementalAmuletItem> EVERTIDE_AMULET =
        ProjectEXContentRegistry.registerItem(
            "evertide_amulet", properties -> new ElementalAmuletItem(
                properties, ElementalAmuletItem.Element.WATER),
            new Item.Properties().rarity(Rarity.RARE).fireResistant()
        );
    public static final ProjectEXContentRegistry.RegisteredItem<ElementalAmuletItem> VOLCANITE_AMULET =
        ProjectEXContentRegistry.registerItem(
            "volcanite_amulet", properties -> new ElementalAmuletItem(
                properties, ElementalAmuletItem.Element.LAVA),
            new Item.Properties().rarity(Rarity.RARE).fireResistant()
        );
    public static final List<ProjectEXContentRegistry.RegisteredItem<DiviningRodItem>> DIVINING_RODS =
        java.util.stream.IntStream.range(0, 3).mapToObj(tier ->
            ProjectEXContentRegistry.registerItem(
                "divining_rod_" + (tier + 1),
                properties -> new DiviningRodItem(properties, tier),
                new Item.Properties().rarity(tier == 2 ? Rarity.RARE : Rarity.UNCOMMON)
            )
        ).toList();
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
    public static final ProjectEXContentRegistry.RegisteredItem<KleinStarItem> MAGNUM_STAR_EIN = star(KleinStarTier.MAGNUM_EIN);
    public static final ProjectEXContentRegistry.RegisteredItem<KleinStarItem> MAGNUM_STAR_ZWEI = star(KleinStarTier.MAGNUM_ZWEI);
    public static final ProjectEXContentRegistry.RegisteredItem<KleinStarItem> MAGNUM_STAR_DREI = star(KleinStarTier.MAGNUM_DREI);
    public static final ProjectEXContentRegistry.RegisteredItem<KleinStarItem> MAGNUM_STAR_VIER = star(KleinStarTier.MAGNUM_VIER);
    public static final ProjectEXContentRegistry.RegisteredItem<KleinStarItem> MAGNUM_STAR_SPHERE = star(KleinStarTier.MAGNUM_SPHERE);
    public static final ProjectEXContentRegistry.RegisteredItem<KleinStarItem> MAGNUM_STAR_OMEGA = star(KleinStarTier.MAGNUM_OMEGA);
    public static final ProjectEXContentRegistry.RegisteredItem<KleinStarItem> COLOSSAL_STAR_EIN = star(KleinStarTier.COLOSSAL_EIN);
    public static final ProjectEXContentRegistry.RegisteredItem<KleinStarItem> COLOSSAL_STAR_ZWEI = star(KleinStarTier.COLOSSAL_ZWEI);
    public static final ProjectEXContentRegistry.RegisteredItem<KleinStarItem> COLOSSAL_STAR_DREI = star(KleinStarTier.COLOSSAL_DREI);
    public static final ProjectEXContentRegistry.RegisteredItem<KleinStarItem> COLOSSAL_STAR_VIER = star(KleinStarTier.COLOSSAL_VIER);
    public static final ProjectEXContentRegistry.RegisteredItem<KleinStarItem> COLOSSAL_STAR_SPHERE = star(KleinStarTier.COLOSSAL_SPHERE);
    public static final ProjectEXContentRegistry.RegisteredItem<KleinStarItem> COLOSSAL_STAR_OMEGA = star(KleinStarTier.COLOSSAL_OMEGA);
    public static final ProjectEXContentRegistry.RegisteredItem<KleinStarItem> GARGANTUAN_STAR_EIN = star(KleinStarTier.GARGANTUAN_EIN);
    public static final ProjectEXContentRegistry.RegisteredItem<KleinStarItem> GARGANTUAN_STAR_ZWEI = star(KleinStarTier.GARGANTUAN_ZWEI);
    public static final ProjectEXContentRegistry.RegisteredItem<KleinStarItem> GARGANTUAN_STAR_DREI = star(KleinStarTier.GARGANTUAN_DREI);
    public static final ProjectEXContentRegistry.RegisteredItem<KleinStarItem> GARGANTUAN_STAR_VIER = star(KleinStarTier.GARGANTUAN_VIER);
    public static final ProjectEXContentRegistry.RegisteredItem<KleinStarItem> GARGANTUAN_STAR_SPHERE = star(KleinStarTier.GARGANTUAN_SPHERE);
    public static final ProjectEXContentRegistry.RegisteredItem<KleinStarItem> GARGANTUAN_STAR_OMEGA = star(KleinStarTier.GARGANTUAN_OMEGA);
    public static final List<ProjectEXContentRegistry.RegisteredItem<AlchemicalBagItem>> ALCHEMICAL_BAGS =
        java.util.Arrays.stream(DyeColor.values()).map(ProjectEXItems::bag).toList();
    public static final ProjectEXContentRegistry.RegisteredItem<MatterToolItem> DARK_MATTER_PICKAXE =
        activeMatterTool("dark_matter_pickaxe", MatterTier.DARK, MatterToolItem.Kind.PICKAXE, DARK_TOOL_MATERIAL, 1, -2.8F);
    public static final ProjectEXContentRegistry.RegisteredItem<MatterToolItem> DARK_MATTER_HAMMER =
        activeMatterTool("dark_matter_hammer", MatterTier.DARK, MatterToolItem.Kind.HAMMER, DARK_TOOL_MATERIAL, 4, -3.2F);
    public static final ProjectEXContentRegistry.RegisteredItem<MatterToolItem> RED_MATTER_PICKAXE =
        activeMatterTool("red_matter_pickaxe", MatterTier.RED, MatterToolItem.Kind.PICKAXE, RED_TOOL_MATERIAL, 1, -2.8F);
    public static final ProjectEXContentRegistry.RegisteredItem<MatterToolItem> RED_MATTER_HAMMER =
        activeMatterTool("red_matter_hammer", MatterTier.RED, MatterToolItem.Kind.HAMMER, RED_TOOL_MATERIAL, 5, -3.1F);
    public static final List<ProjectEXContentRegistry.RegisteredItem<Item>> MATTER_HAND_TOOLS = List.of(
        handTool("dark_matter_axe", DARK_TOOL_MATERIAL, "axe"),
        handTool("dark_matter_shovel", DARK_TOOL_MATERIAL, "shovel"),
        handTool("dark_matter_hoe", DARK_TOOL_MATERIAL, "hoe"),
        handTool("dark_matter_sword", DARK_TOOL_MATERIAL, "sword"),
        handTool("red_matter_axe", RED_TOOL_MATERIAL, "axe"),
        handTool("red_matter_shovel", RED_TOOL_MATERIAL, "shovel"),
        handTool("red_matter_hoe", RED_TOOL_MATERIAL, "hoe"),
        handTool("red_matter_sword", RED_TOOL_MATERIAL, "sword")
    );
    public static final List<ProjectEXContentRegistry.RegisteredItem<MatterArmorItem>> MATTER_ARMOR =
        java.util.stream.Stream.concat(
            java.util.Arrays.stream(ArmorType.values()).filter(type -> type != ArmorType.BODY)
                .map(type -> armor("dark_matter_" + armorName(type), MatterTier.DARK, type, DARK_ARMOR_MATERIAL)),
            java.util.Arrays.stream(ArmorType.values()).filter(type -> type != ArmorType.BODY)
                .map(type -> armor("red_matter_" + armorName(type), MatterTier.RED, type, RED_ARMOR_MATERIAL))
        ).toList();

    private static final List<ProjectEXContentRegistry.RegisteredItem<Item>> MATERIALS =
        java.util.stream.Stream.concat(
            java.util.stream.Stream.of(
                LOW_COVALENCE_DUST, MEDIUM_COVALENCE_DUST, HIGH_COVALENCE_DUST,
                ALCHEMICAL_COAL, MOBIUS_FUEL, AETERNALIS_FUEL, DARK_MATTER, RED_MATTER
            ),
            java.util.stream.Stream.concat(
                EXPANSION_FUELS.stream(),
                java.util.stream.Stream.concat(EXPANSION_MATTERS.stream(), java.util.stream.Stream.of(FADING_MATTER))
            )
        ).toList();
    private static final List<ProjectEXContentRegistry.RegisteredItem<KleinStarItem>> KLEIN_STARS =
        List.of(
            KLEIN_STAR_EIN,
            KLEIN_STAR_ZWEI,
            KLEIN_STAR_DREI,
            KLEIN_STAR_VIER,
            KLEIN_STAR_SPHERE,
            KLEIN_STAR_OMEGA,
            MAGNUM_STAR_EIN, MAGNUM_STAR_ZWEI, MAGNUM_STAR_DREI, MAGNUM_STAR_VIER,
            MAGNUM_STAR_SPHERE, MAGNUM_STAR_OMEGA,
            COLOSSAL_STAR_EIN, COLOSSAL_STAR_ZWEI, COLOSSAL_STAR_DREI, COLOSSAL_STAR_VIER,
            COLOSSAL_STAR_SPHERE, COLOSSAL_STAR_OMEGA,
            GARGANTUAN_STAR_EIN, GARGANTUAN_STAR_ZWEI, GARGANTUAN_STAR_DREI, GARGANTUAN_STAR_VIER,
            GARGANTUAN_STAR_SPHERE, GARGANTUAN_STAR_OMEGA
        );

    private ProjectEXItems() {
    }

    public static void register() {
        FuelValueEvents.BUILD.register((builder, context) -> {
            builder.add(ALCHEMICAL_COAL.item(), 1_600);
            builder.add(MOBIUS_FUEL.item(), 6_400);
            builder.add(AETERNALIS_FUEL.item(), 25_600);
            EXPANSION_FUELS.forEach(fuel -> builder.add(fuel.item(), 25_600));
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
            .register(entries -> MATERIALS.forEach(entry -> entries.accept(entry.item())));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
            .register(entries -> {
                entries.accept(PHILOSOPHERS_STONE.item());
                entries.accept(TRANSMUTATION_TABLET.item());
                entries.accept(REPAIR_TALISMAN.item());
                entries.accept(EVERTIDE_AMULET.item());
                entries.accept(VOLCANITE_AMULET.item());
                DIVINING_RODS.forEach(entry -> entries.accept(entry.item()));
                entries.accept(FINAL_STAR_SHARD.item());
                entries.accept(FINAL_STAR.item());
                entries.accept(INFINITE_STEAK.item());
                KLEIN_STARS.forEach(entry -> entries.accept(entry.item()));
                COMPRESSED_COLLECTORS.forEach(entry -> entries.accept(entry.item()));
                ALCHEMICAL_BAGS.forEach(entry -> entries.accept(entry.item()));
                entries.accept(DARK_MATTER_PICKAXE.item());
                entries.accept(DARK_MATTER_HAMMER.item());
                entries.accept(RED_MATTER_PICKAXE.item());
                entries.accept(RED_MATTER_HAMMER.item());
                MATTER_HAND_TOOLS.forEach(entry -> entries.accept(entry.item()));
                MATTER_ARMOR.forEach(entry -> entries.accept(entry.item()));
            });
    }

    public static List<ProjectEXContentRegistry.RegisteredItem<Item>> materials() {
        return MATERIALS;
    }

    public static List<ProjectEXContentRegistry.RegisteredItem<KleinStarItem>> kleinStars() {
        return KLEIN_STARS;
    }

    public static ProjectEXContentRegistry.RegisteredItem<KleinStarItem> kleinStar(KleinStarTier tier) {
        return KLEIN_STARS.get(tier.ordinal());
    }

    public static List<ProjectEXContentRegistry.RegisteredItem<AlchemicalBagItem>> alchemicalBags() {
        return ALCHEMICAL_BAGS;
    }

    private static ProjectEXContentRegistry.RegisteredItem<Item> simple(String path) {
        return ProjectEXContentRegistry.registerItem(path, Item::new, new Item.Properties());
    }

    private static ProjectEXContentRegistry.RegisteredItem<Item> material(String path, Rarity rarity) {
        return ProjectEXContentRegistry.registerItem(
            path, Item::new, new Item.Properties().rarity(rarity).fireResistant()
        );
    }

    private static ProjectEXContentRegistry.RegisteredItem<KleinStarItem> star(KleinStarTier tier) {
        return ProjectEXContentRegistry.registerItem(
            tier.serializedName(),
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

    private static ProjectEXContentRegistry.RegisteredItem<MatterToolItem> activeMatterTool(
        String id, MatterTier tier, MatterToolItem.Kind kind, ToolMaterial material,
        float attackDamage, float attackSpeed
    ) {
        return ProjectEXContentRegistry.registerItem(
            id, properties -> new MatterToolItem(properties, tier, kind),
            new Item.Properties().rarity(tier == MatterTier.RED ? Rarity.EPIC : Rarity.RARE)
                .pickaxe(material, attackDamage, attackSpeed).fireResistant()
        );
    }

    private static ProjectEXContentRegistry.RegisteredItem<Item> handTool(
        String id, ToolMaterial material, String kind
    ) {
        Item.Properties properties = new Item.Properties().rarity(Rarity.RARE).fireResistant();
        properties = switch (kind) {
            case "axe" -> properties.axe(material, 5, -3.0F);
            case "shovel" -> properties.shovel(material, 1.5F, -3.0F);
            case "hoe" -> properties.hoe(material, -2, 0);
            case "sword" -> properties.sword(material, 3, -2.4F);
            default -> throw new IllegalArgumentException("Unknown matter tool kind");
        };
        return ProjectEXContentRegistry.registerItem(id, Item::new, properties);
    }

    private static ProjectEXContentRegistry.RegisteredItem<MatterArmorItem> armor(
        String id, MatterTier tier, ArmorType type, ArmorMaterial material
    ) {
        return ProjectEXContentRegistry.registerItem(
            id, properties -> new MatterArmorItem(properties, tier, type),
            new Item.Properties().rarity(tier == MatterTier.RED ? Rarity.EPIC : Rarity.RARE)
                .fireResistant().humanoidArmor(material, type)
        );
    }

    private static ArmorMaterial armorMaterial(
        int durability, int helmet, int chest, int legs, int boots,
        float toughness, float knockback, net.minecraft.tags.TagKey<Item> repair
    ) {
        return new ArmorMaterial(
            durability,
            Map.of(ArmorType.HELMET, helmet, ArmorType.CHESTPLATE, chest,
                ArmorType.LEGGINGS, legs, ArmorType.BOOTS, boots, ArmorType.BODY, chest),
            1, SoundEvents.ARMOR_EQUIP_NETHERITE, toughness, knockback, repair,
            EquipmentAssets.NETHERITE
        );
    }

    private static String armorName(ArmorType type) {
        return switch (type) {
            case HELMET -> "helmet";
            case CHESTPLATE -> "chestplate";
            case LEGGINGS -> "leggings";
            case BOOTS -> "boots";
            case BODY -> throw new IllegalArgumentException("Body armor is unsupported");
        };
    }
}
