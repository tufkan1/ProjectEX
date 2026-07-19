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
        Map.entry("philosophers_stone", "minecraft:item/ender_eye")
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
