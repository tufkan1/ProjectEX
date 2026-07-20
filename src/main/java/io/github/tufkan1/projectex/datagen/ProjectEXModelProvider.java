package io.github.tufkan1.projectex.datagen;

import com.google.gson.JsonParser;
import io.github.tufkan1.projectex.ProjectEX;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/** Deterministically emits the block, block-state, and 26.2 client item model set. */
public final class ProjectEXModelProvider implements DataProvider {
    private static final String BLOCK_MODEL = """
        { "parent": "projecte:block/transmutation_table" }
        """;
    private static final String BLOCK_STATE = """
        {
          "variants": {
            "": {
              "model": "projectex:block/transmutation_table"
            }
          }
        }
        """;
    private static final String ITEM_MODEL = """
        {
          "model": {
            "type": "minecraft:model",
            "model": "projectex:block/transmutation_table"
          }
        }
        """;
    private static final Map<String, String> GENERATED_ITEMS = Map.ofEntries(
        Map.entry("low_covalence_dust", "projecte:item/covalence_dust/low"),
        Map.entry("medium_covalence_dust", "projecte:item/covalence_dust/medium"),
        Map.entry("high_covalence_dust", "projecte:item/covalence_dust/high"),
        Map.entry("alchemical_coal", "projecte:item/fuels/alchemical_coal"),
        Map.entry("mobius_fuel", "projecte:item/fuels/mobius"),
        Map.entry("aeternalis_fuel", "projecte:item/fuels/aeternalis"),
        Map.entry("dark_matter", "projecte:item/matter/dark"),
        Map.entry("red_matter", "projecte:item/matter/red"),
        Map.entry("magenta_fuel", "projectexpansion:item/fuel/magenta"),
        Map.entry("pink_fuel", "projectexpansion:item/fuel/pink"),
        Map.entry("purple_fuel", "projectexpansion:item/fuel/purple"),
        Map.entry("violet_fuel", "projectexpansion:item/fuel/violet"),
        Map.entry("blue_fuel", "projectexpansion:item/fuel/blue"),
        Map.entry("cyan_fuel", "projectexpansion:item/fuel/cyan"),
        Map.entry("green_fuel", "projectexpansion:item/fuel/green"),
        Map.entry("lime_fuel", "projectexpansion:item/fuel/lime"),
        Map.entry("yellow_fuel", "projectexpansion:item/fuel/yellow"),
        Map.entry("orange_fuel", "projectexpansion:item/fuel/orange"),
        Map.entry("white_fuel", "projectexpansion:item/fuel/white"),
        Map.entry("magenta_matter", "projectexpansion:item/matter/magenta"),
        Map.entry("pink_matter", "projectexpansion:item/matter/pink"),
        Map.entry("purple_matter", "projectexpansion:item/matter/purple"),
        Map.entry("violet_matter", "projectexpansion:item/matter/violet"),
        Map.entry("blue_matter", "projectexpansion:item/matter/blue"),
        Map.entry("cyan_matter", "projectexpansion:item/matter/cyan"),
        Map.entry("green_matter", "projectexpansion:item/matter/green"),
        Map.entry("lime_matter", "projectexpansion:item/matter/lime"),
        Map.entry("yellow_matter", "projectexpansion:item/matter/yellow"),
        Map.entry("orange_matter", "projectexpansion:item/matter/orange"),
        Map.entry("white_matter", "projectexpansion:item/matter/white"),
        Map.entry("fading_matter", "projectexpansion:item/matter/fading"),
        Map.entry("final_star_shard", "projectexpansion:item/star/final_shard"),
        Map.entry("final_star", "projectexpansion:item/star/final"),
        Map.entry("infinite_steak", "projectexpansion:item/infinite_steak"),
        Map.entry("philosophers_stone", "projecte:item/philosophers_stone"),
        Map.entry("transmutation_tablet", "projecte:item/transmutation_tablet"),
        Map.entry("arcane_tablet", "projectexpansion:item/arcane_transmutation_tablet"),
        Map.entry("repair_talisman", "projecte:item/repair_talisman"),
        Map.entry("evertide_amulet", "projecte:item/rings/evertide_amulet"),
        Map.entry("volcanite_amulet", "projecte:item/rings/volcanite_amulet"),
        Map.entry("knowledge_tome", "projecte:item/tome"),
        Map.entry("knowledge_sharing_book", "projectexpansion:item/knowledge_sharing_book"),
        Map.entry("basic_alchemical_book", "projectexpansion:item/basic_alchemical_book"),
        Map.entry("advanced_alchemical_book", "projectexpansion:item/advanced_alchemical_book"),
        Map.entry("master_alchemical_book", "projectexpansion:item/master_alchemical_book"),
        Map.entry("arcane_alchemical_book", "projectexpansion:item/arcane_alchemical_book"),
        Map.entry("nova_catalyst", "projecte:block/explosives/nova_side"),
        Map.entry("destruction_catalyst", "projecte:item/destruction_catalyst"),
        Map.entry("body_stone", "projecte:item/rings/body_stone_off"),
        Map.entry("soul_stone", "projecte:item/rings/soul_stone_off"),
        Map.entry("life_stone", "projecte:item/rings/life_stone_off"),
        Map.entry("divining_rod_1", "projecte:item/divining_rod_1"),
        Map.entry("divining_rod_2", "projecte:item/divining_rod_2"),
        Map.entry("divining_rod_3", "projecte:item/divining_rod_3"),
        Map.entry("klein_star_ein", "projecte:item/stars/klein_star_1"),
        Map.entry("klein_star_zwei", "projecte:item/stars/klein_star_2"),
        Map.entry("klein_star_drei", "projecte:item/stars/klein_star_3"),
        Map.entry("klein_star_vier", "projecte:item/stars/klein_star_4"),
        Map.entry("klein_star_sphere", "projecte:item/stars/klein_star_5"),
        Map.entry("klein_star_omega", "projecte:item/stars/klein_star_6"),
        Map.entry("magnum_star_ein", "projectexpansion:item/star/magnum/ein"),
        Map.entry("magnum_star_zwei", "projectexpansion:item/star/magnum/zwei"),
        Map.entry("magnum_star_drei", "projectexpansion:item/star/magnum/drei"),
        Map.entry("magnum_star_vier", "projectexpansion:item/star/magnum/vier"),
        Map.entry("magnum_star_sphere", "projectexpansion:item/star/magnum/sphere"),
        Map.entry("magnum_star_omega", "projectexpansion:item/star/magnum/omega"),
        Map.entry("colossal_star_ein", "projectexpansion:item/star/colossal/ein"),
        Map.entry("colossal_star_zwei", "projectexpansion:item/star/colossal/zwei"),
        Map.entry("colossal_star_drei", "projectexpansion:item/star/colossal/drei"),
        Map.entry("colossal_star_vier", "projectexpansion:item/star/colossal/vier"),
        Map.entry("colossal_star_sphere", "projectexpansion:item/star/colossal/sphere"),
        Map.entry("colossal_star_omega", "projectexpansion:item/star/colossal/omega"),
        Map.entry("gargantuan_star_ein", "projectexpansion:item/star/gargantuan/ein"),
        Map.entry("gargantuan_star_zwei", "projectexpansion:item/star/gargantuan/zwei"),
        Map.entry("gargantuan_star_drei", "projectexpansion:item/star/gargantuan/drei"),
        Map.entry("gargantuan_star_vier", "projectexpansion:item/star/gargantuan/vier"),
        Map.entry("gargantuan_star_sphere", "projectexpansion:item/star/gargantuan/sphere"),
        Map.entry("gargantuan_star_omega", "projectexpansion:item/star/gargantuan/omega")
    );
    private static final Map<String, String> MACHINE_BLOCKS = Map.ofEntries(
        Map.entry("collector_mk1", "projecte:block/collector_mk1"),
        Map.entry("collector_mk2", "projecte:block/collector_mk2"),
        Map.entry("collector_mk3", "projecte:block/collector_mk3"),
        Map.entry("relay_mk1", "projecte:block/relay_mk1"),
        Map.entry("relay_mk2", "projecte:block/relay_mk2"),
        Map.entry("relay_mk3", "projecte:block/relay_mk3"),
        Map.entry("condenser_mk1", "projecte:block/condenser_mk1"),
        Map.entry("condenser_mk2", "projecte:block/condenser_mk2"),
        Map.entry("condenser_mk3", "projectexpansion:block/condenser_mk3"),
        Map.entry("alchemical_chest", "projecte:block/alchemical_chest"),
        Map.entry("advanced_alchemical_chest", "projectexpansion:block/advanced_alchemical_chest/purple"),
        Map.entry("dark_matter_block", "projecte:block/dark_matter_block"),
        Map.entry("red_matter_block", "projecte:block/red_matter_block"),
        Map.entry("alchemical_coal_block", "projecte:block/alchemical_coal_block"),
        Map.entry("mobius_fuel_block", "projecte:block/mobius_fuel_block"),
        Map.entry("aeternalis_fuel_block", "projecte:block/aeternalis_fuel_block"),
        Map.entry("dark_matter_furnace", "projecte:block/dm_furnace"),
        Map.entry("red_matter_furnace", "projecte:block/rm_furnace"),
        Map.entry("dark_matter_pedestal", "projecte:block/dm_pedestal")
    );
    private static final Map<String, String> STORAGE_PARTICLES = Map.of(
        "condenser_mk1", "projecte:block/condenser_mk1",
        "condenser_mk2", "projecte:block/condenser_mk2",
        "condenser_mk3", "projectexpansion:block/condenser_mk3",
        "alchemical_chest", "projecte:block/alchemical_chest",
        "advanced_alchemical_chest", "projectexpansion:block/advanced_alchemical_chest/purple"
    );

    private final Path assetsRoot;

    public ProjectEXModelProvider(FabricPackOutput output) {
        assetsRoot = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
            .resolve(ProjectEX.MOD_ID);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        writes.add(save(output, "models/block/transmutation_table.json", BLOCK_MODEL));
        writes.add(save(output, "blockstates/transmutation_table.json", BLOCK_STATE));
        writes.add(save(output, "items/transmutation_table.json", ITEM_MODEL));
        Map<String, String> machineBlocks = new java.util.LinkedHashMap<>(MACHINE_BLOCKS);
        io.github.tufkan1.projectex.content.ProjectEXBlocks.EXPANSION_COLLECTORS.forEach((tier, entry) ->
            machineBlocks.put(entry.id().getPath(), "projectexpansion:block/collector/" + tier.id()));
        io.github.tufkan1.projectex.content.ProjectEXBlocks.EXPANSION_RELAYS.forEach((tier, entry) ->
            machineBlocks.put(entry.id().getPath(), "projectexpansion:block/relay/" + tier.id()));
        io.github.tufkan1.projectex.content.ProjectEXBlocks.POWER_FLOWERS.forEach((tier, entry) ->
            machineBlocks.put(entry.id().getPath(), "projectexpansion:block/power_flower/" + tier.id()));
        machineBlocks.put("compact_sun", "projectexpansion:block/compact_sun");
        io.github.tufkan1.projectex.content.ProjectEXBlocks.EMC_LINKS.forEach((tier, entry) ->
            machineBlocks.put(entry.id().getPath(), "projectexpansion:block/emc_link/" + tier.id())
        );
        machineBlocks.put("transmutation_interface", "projectexpansion:block/transmutation_interface");
        machineBlocks.forEach((block, sourceModel) -> {
            boolean storage = block.startsWith("condenser_")
                || block.equals("alchemical_chest") || block.equals("advanced_alchemical_chest");
            String blockModel = storage ? """
                {
                  "parent": "%s",
                  "textures": { "particle": "%s" }
                }
                """.formatted(sourceModel, STORAGE_PARTICLES.get(block)) : """
                { "parent": "%s" }
                """.formatted(sourceModel);
            String blockState = storage ? """
                {
                  "variants": {
                    "facing=north": { "model": "projectex:block/%1$s" },
                    "facing=east": { "model": "projectex:block/%1$s", "y": 90 },
                    "facing=south": { "model": "projectex:block/%1$s", "y": 180 },
                    "facing=west": { "model": "projectex:block/%1$s", "y": 270 }
                  }
                }
                """.formatted(block) : """
                {
                  "variants": {
                    "": {
                      "model": "projectex:block/%s"
                    }
                  }
                }
                """.formatted(block);
            String clientItem = """
                {
                  "model": {
                    "type": "minecraft:model",
                    "model": "projectex:block/%s"
                  }
                }
                """.formatted(block);
            writes.add(save(output, "models/block/" + block + ".json", blockModel));
            writes.add(save(output, "blockstates/" + block + ".json", blockState));
            writes.add(save(output, "items/" + block + ".json", clientItem));
        });
        GENERATED_ITEMS.forEach((item, texture) -> {
            String baseModel = """
                {
                  "parent": "minecraft:item/generated",
                  "textures": {
                    "layer0": "%s"
                  }
                }
                """.formatted(texture);
            String clientItem = """
                {
                  "model": {
                    "type": "minecraft:model",
                    "model": "projectex:item/%s"
                  }
                }
                """.formatted(item);
            writes.add(save(output, "models/item/" + item + ".json", baseModel));
            writes.add(save(output, "items/" + item + ".json", clientItem));
        });
        io.github.tufkan1.projectex.content.ProjectEXItems.COMPRESSED_COLLECTORS.forEach(entry -> {
            String item = entry.id().getPath();
            String tier = item.substring(0, item.indexOf("_compressed_collector"));
            String baseModel = """
                { "parent": "projectexpansion:block/collector/%s" }
                """.formatted(tier);
            String clientItem = """
                {
                  "model": { "type": "minecraft:model", "model": "projectex:item/%s" }
                }
                """.formatted(item);
            writes.add(save(output, "models/item/" + item + ".json", baseModel));
            writes.add(save(output, "items/" + item + ".json", clientItem));
        });
        for (net.minecraft.world.item.DyeColor color : net.minecraft.world.item.DyeColor.values()) {
            String item = color.getName() + "_alchemical_bag";
            String baseModel = """
                {
                  "parent": "minecraft:item/generated",
                  "textures": { "layer0": "projecte:item/alchemy_bags/%s" }
                }
                """.formatted(color.getName());
            String clientItem = """
                {
                  "model": { "type": "minecraft:model", "model": "projectex:item/%s" }
                }
                """.formatted(item);
            writes.add(save(output, "models/item/" + item + ".json", baseModel));
            writes.add(save(output, "items/" + item + ".json", clientItem));
        }
        java.util.stream.Stream.concat(
            java.util.stream.Stream.of(
                io.github.tufkan1.projectex.content.ProjectEXItems.DARK_MATTER_PICKAXE,
                io.github.tufkan1.projectex.content.ProjectEXItems.DARK_MATTER_HAMMER,
                io.github.tufkan1.projectex.content.ProjectEXItems.RED_MATTER_PICKAXE,
                io.github.tufkan1.projectex.content.ProjectEXItems.RED_MATTER_HAMMER
            ),
            java.util.stream.Stream.concat(
                io.github.tufkan1.projectex.content.ProjectEXItems.MATTER_HAND_TOOLS.stream(),
                io.github.tufkan1.projectex.content.ProjectEXItems.MATTER_ARMOR.stream()
            )
        ).forEach(entry -> {
            String item = entry.id().getPath();
            String suffix = item.substring(item.indexOf("matter_") + "matter_".length());
            boolean dark = item.startsWith("dark_matter_");
            String sourceSuffix = switch (suffix) {
                case "helmet" -> "head";
                case "chestplate" -> "chest";
                case "leggings" -> "legs";
                case "boots" -> "feet";
                default -> suffix;
            };
            String texture = "projecte:item/" + (dark ? "dm_" : "rm_")
                + (switch (suffix) {
                    case "helmet", "chestplate", "leggings", "boots" -> "armor/";
                    default -> "tools/";
                }) + sourceSuffix;
            String parent = switch (suffix) {
                case "helmet", "chestplate", "leggings", "boots" -> "minecraft:item/generated";
                default -> "minecraft:item/handheld";
            };
            String baseModel = """
                {
                  "parent": "%s",
                  "textures": { "layer0": "%s" }
                }
                """.formatted(parent, texture);
            String clientItem = """
                {
                  "model": { "type": "minecraft:model", "model": "projectex:item/%s" }
                }
                """.formatted(item);
            writes.add(save(output, "models/item/" + item + ".json", baseModel));
            writes.add(save(output, "items/" + item + ".json", clientItem));
        });
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    private CompletableFuture<?> save(CachedOutput output, String path, String json) {
        return DataProvider.saveStable(output, JsonParser.parseString(json), assetsRoot.resolve(path));
    }

    @Override
    public String getName() {
        return "ProjectEX models";
    }
}
