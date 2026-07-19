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
        {
          "parent": "minecraft:block/cube_bottom_top",
          "textures": {
            "bottom": "minecraft:block/obsidian",
            "side": "minecraft:block/polished_blackstone",
            "top": "minecraft:block/diamond_block"
          }
        }
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
        Map.entry("low_covalence_dust", "minecraft:item/gunpowder"),
        Map.entry("medium_covalence_dust", "minecraft:item/redstone"),
        Map.entry("high_covalence_dust", "minecraft:item/glowstone_dust"),
        Map.entry("alchemical_coal", "minecraft:item/coal"),
        Map.entry("mobius_fuel", "minecraft:item/blaze_powder"),
        Map.entry("aeternalis_fuel", "minecraft:item/echo_shard"),
        Map.entry("dark_matter", "minecraft:item/ender_pearl"),
        Map.entry("red_matter", "minecraft:item/nether_star"),
        Map.entry("magenta_fuel", "minecraft:item/magenta_dye"),
        Map.entry("pink_fuel", "minecraft:item/pink_dye"),
        Map.entry("purple_fuel", "minecraft:item/purple_dye"),
        Map.entry("violet_fuel", "minecraft:item/chorus_fruit"),
        Map.entry("blue_fuel", "minecraft:item/blue_dye"),
        Map.entry("cyan_fuel", "minecraft:item/cyan_dye"),
        Map.entry("green_fuel", "minecraft:item/green_dye"),
        Map.entry("lime_fuel", "minecraft:item/lime_dye"),
        Map.entry("yellow_fuel", "minecraft:item/yellow_dye"),
        Map.entry("orange_fuel", "minecraft:item/orange_dye"),
        Map.entry("white_fuel", "minecraft:item/white_dye"),
        Map.entry("magenta_matter", "minecraft:item/magenta_dye"),
        Map.entry("pink_matter", "minecraft:item/pink_dye"),
        Map.entry("purple_matter", "minecraft:item/purple_dye"),
        Map.entry("violet_matter", "minecraft:item/chorus_fruit"),
        Map.entry("blue_matter", "minecraft:item/blue_dye"),
        Map.entry("cyan_matter", "minecraft:item/cyan_dye"),
        Map.entry("green_matter", "minecraft:item/green_dye"),
        Map.entry("lime_matter", "minecraft:item/lime_dye"),
        Map.entry("yellow_matter", "minecraft:item/yellow_dye"),
        Map.entry("orange_matter", "minecraft:item/orange_dye"),
        Map.entry("white_matter", "minecraft:item/quartz"),
        Map.entry("fading_matter", "minecraft:item/echo_shard"),
        Map.entry("final_star_shard", "minecraft:item/nether_star"),
        Map.entry("final_star", "minecraft:block/dragon_egg"),
        Map.entry("infinite_steak", "minecraft:item/cooked_beef"),
        Map.entry("philosophers_stone", "minecraft:item/ender_eye"),
        Map.entry("transmutation_tablet", "minecraft:item/enchanted_book"),
        Map.entry("klein_star_ein", "minecraft:item/amethyst_shard"),
        Map.entry("klein_star_zwei", "minecraft:item/prismarine_crystals"),
        Map.entry("klein_star_drei", "minecraft:item/diamond"),
        Map.entry("klein_star_vier", "minecraft:item/emerald"),
        Map.entry("klein_star_sphere", "minecraft:item/heart_of_the_sea"),
        Map.entry("klein_star_omega", "minecraft:item/nether_star"),
        Map.entry("magnum_star_ein", "minecraft:item/amethyst_shard"),
        Map.entry("magnum_star_zwei", "minecraft:item/prismarine_crystals"),
        Map.entry("magnum_star_drei", "minecraft:item/diamond"),
        Map.entry("magnum_star_vier", "minecraft:item/emerald"),
        Map.entry("magnum_star_sphere", "minecraft:item/heart_of_the_sea"),
        Map.entry("magnum_star_omega", "minecraft:item/nether_star"),
        Map.entry("colossal_star_ein", "minecraft:item/amethyst_shard"),
        Map.entry("colossal_star_zwei", "minecraft:item/prismarine_crystals"),
        Map.entry("colossal_star_drei", "minecraft:item/diamond"),
        Map.entry("colossal_star_vier", "minecraft:item/emerald"),
        Map.entry("colossal_star_sphere", "minecraft:item/heart_of_the_sea"),
        Map.entry("colossal_star_omega", "minecraft:item/nether_star"),
        Map.entry("gargantuan_star_ein", "minecraft:item/amethyst_shard"),
        Map.entry("gargantuan_star_zwei", "minecraft:item/prismarine_crystals"),
        Map.entry("gargantuan_star_drei", "minecraft:item/diamond"),
        Map.entry("gargantuan_star_vier", "minecraft:item/emerald"),
        Map.entry("gargantuan_star_sphere", "minecraft:item/heart_of_the_sea"),
        Map.entry("gargantuan_star_omega", "minecraft:item/nether_star")
    );
    private static final Map<String, String> MACHINE_BLOCKS = Map.ofEntries(
        Map.entry("collector_mk1", "minecraft:block/glowstone"),
        Map.entry("collector_mk2", "minecraft:block/diamond_block"),
        Map.entry("collector_mk3", "minecraft:block/emerald_block"),
        Map.entry("relay_mk1", "minecraft:block/obsidian"),
        Map.entry("relay_mk2", "minecraft:block/polished_blackstone"),
        Map.entry("relay_mk3", "minecraft:block/crying_obsidian"),
        Map.entry("condenser_mk1", "minecraft:block/diamond_block"),
        Map.entry("condenser_mk2", "minecraft:block/netherite_block"),
        Map.entry("alchemical_chest", "minecraft:block/obsidian"),
        Map.entry("dark_matter_block", "minecraft:block/coal_block"),
        Map.entry("red_matter_block", "minecraft:block/redstone_block")
        , Map.entry("dark_matter_furnace", "minecraft:block/blast_furnace_side")
        , Map.entry("red_matter_furnace", "minecraft:block/crying_obsidian")
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
        java.util.stream.Stream.of(
            io.github.tufkan1.projectex.content.ProjectEXBlocks.EXPANSION_COLLECTORS,
            io.github.tufkan1.projectex.content.ProjectEXBlocks.EXPANSION_RELAYS,
            io.github.tufkan1.projectex.content.ProjectEXBlocks.POWER_FLOWERS
        ).flatMap(map -> map.entrySet().stream()).forEach(entry ->
            machineBlocks.put(entry.getValue().id().getPath(), expansionTexture(entry.getKey()))
        );
        machineBlocks.put("compact_sun", "minecraft:block/shroomlight");
        io.github.tufkan1.projectex.content.ProjectEXBlocks.EMC_LINKS.forEach((tier, entry) ->
            machineBlocks.put(entry.id().getPath(), expansionTexture(tier))
        );
        machineBlocks.put("transmutation_interface", "minecraft:block/enchanting_table_side");
        machineBlocks.forEach((block, texture) -> {
            String blockModel = """
                {
                  "parent": "minecraft:block/cube_all",
                  "textures": {
                    "all": "%s"
                  }
                }
                """.formatted(texture);
            String blockState = """
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
                {
                  "parent": "minecraft:item/generated",
                  "textures": { "layer0": "%s" }
                }
                """.formatted(expansionTexture(
                    io.github.tufkan1.projectex.machine.ExpansionMachineTier.valueOf(
                        tier.toUpperCase(java.util.Locale.ROOT)
                    )
                ));
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
                  "textures": { "layer0": "minecraft:block/%s_wool" }
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
            String vanilla = suffix.equals("hammer") ? "pickaxe" : suffix;
            String texture = "minecraft:item/netherite_" + vanilla;
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

    private static String expansionTexture(
        io.github.tufkan1.projectex.machine.ExpansionMachineTier tier
    ) {
        return switch (tier) {
            case BASIC -> "minecraft:block/glowstone";
            case DARK -> "minecraft:block/coal_block";
            case RED -> "minecraft:block/redstone_block";
            case VIOLET -> "minecraft:block/chorus_plant";
            case FADING -> "minecraft:block/crying_obsidian";
            case FINAL -> "minecraft:block/netherite_block";
            default -> "minecraft:block/" + tier.id() + "_concrete";
        };
    }

    @Override
    public String getName() {
        return "ProjectEX models";
    }
}
