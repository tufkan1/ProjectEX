package io.github.tufkan1.projectex.datagen;

import com.google.gson.JsonParser;
import io.github.tufkan1.projectex.ProjectEX;
import java.nio.file.Path;
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

    private final Path assetsRoot;

    public ProjectEXModelProvider(FabricPackOutput output) {
        assetsRoot = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
            .resolve(ProjectEX.MOD_ID);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        return CompletableFuture.allOf(
            save(output, "models/block/transmutation_table.json", BLOCK_MODEL),
            save(output, "blockstates/transmutation_table.json", BLOCK_STATE),
            save(output, "items/transmutation_table.json", ITEM_MODEL)
        );
    }

    private CompletableFuture<?> save(CachedOutput output, String path, String json) {
        return DataProvider.saveStable(output, JsonParser.parseString(json), assetsRoot.resolve(path));
    }

    @Override
    public String getName() {
        return "ProjectEX models";
    }
}
