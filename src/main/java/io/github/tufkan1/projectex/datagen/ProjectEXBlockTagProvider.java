package io.github.tufkan1.projectex.datagen;

import io.github.tufkan1.projectex.content.ProjectEXBlocks;
import io.github.tufkan1.projectex.content.ProjectEXTags;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Mining requirements for ProjectEX blocks. */
public final class ProjectEXBlockTagProvider extends FabricTagsProvider.BlockTagsProvider {
    public ProjectEXBlockTagProvider(
        FabricPackOutput output,
        CompletableFuture<HolderLookup.Provider> registriesFuture
    ) {
        super(output, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        ResourceKey<net.minecraft.world.level.block.Block> table = ResourceKey.create(
            Registries.BLOCK,
            ProjectEXBlocks.TRANSMUTATION_TABLE_FAMILY.id()
        );
        builder(BlockTags.MINEABLE_WITH_PICKAXE).add(table);
        builder(BlockTags.NEEDS_DIAMOND_TOOL).add(table);
        builder(ProjectEXTags.PHILOSOPHERS_STONE_ALLOWED).add(
            key(Blocks.STONE),
            key(Blocks.COBBLESTONE),
            key(Blocks.GRAVEL),
            key(Blocks.SAND),
            key(Blocks.GRASS_BLOCK),
            key(Blocks.DIRT)
        );
        builder(ProjectEXTags.PHILOSOPHERS_STONE_DENIED).add(
            key(Blocks.BEDROCK),
            key(Blocks.BARRIER),
            key(Blocks.COMMAND_BLOCK),
            key(Blocks.REPEATING_COMMAND_BLOCK),
            key(Blocks.CHAIN_COMMAND_BLOCK),
            key(Blocks.STRUCTURE_BLOCK),
            key(Blocks.JIGSAW),
            key(Blocks.END_PORTAL_FRAME)
        );
    }

    private static ResourceKey<Block> key(Block block) {
        return BuiltInRegistries.BLOCK.getResourceKey(block).orElseThrow();
    }
}
