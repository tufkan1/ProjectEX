package io.github.tufkan1.projectex.datagen;

import io.github.tufkan1.projectex.content.ProjectEXBlocks;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;

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
    }
}
