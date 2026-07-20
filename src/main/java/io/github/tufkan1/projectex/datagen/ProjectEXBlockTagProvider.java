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
        builder(BlockTags.MINEABLE_WITH_PICKAXE).add(
            key(ProjectEXBlocks.COLLECTOR_MK1), key(ProjectEXBlocks.COLLECTOR_MK2),
            key(ProjectEXBlocks.COLLECTOR_MK3), key(ProjectEXBlocks.RELAY_MK1),
            key(ProjectEXBlocks.RELAY_MK2), key(ProjectEXBlocks.RELAY_MK3)
            , key(ProjectEXBlocks.CONDENSER_MK1), key(ProjectEXBlocks.CONDENSER_MK2),
            key(ProjectEXBlocks.CONDENSER_MK3), key(ProjectEXBlocks.ALCHEMICAL_CHEST),
            key(ProjectEXBlocks.ADVANCED_ALCHEMICAL_CHEST)
            , key(ProjectEXBlocks.DARK_MATTER_BLOCK), key(ProjectEXBlocks.RED_MATTER_BLOCK),
            key(ProjectEXBlocks.ALCHEMICAL_COAL_BLOCK), key(ProjectEXBlocks.MOBIUS_FUEL_BLOCK),
            key(ProjectEXBlocks.AETERNALIS_FUEL_BLOCK),
            key(ProjectEXBlocks.DARK_MATTER_FURNACE), key(ProjectEXBlocks.RED_MATTER_FURNACE)
            , key(ProjectEXBlocks.DARK_MATTER_PEDESTAL)
        );
        builder(BlockTags.MINEABLE_WITH_PICKAXE).add(expansionMachineKeys());
        builder(BlockTags.MINEABLE_WITH_PICKAXE).add(automationKeys());
        builder(BlockTags.NEEDS_DIAMOND_TOOL).add(table);
        builder(BlockTags.NEEDS_DIAMOND_TOOL).add(
            key(ProjectEXBlocks.COLLECTOR_MK1), key(ProjectEXBlocks.COLLECTOR_MK2),
            key(ProjectEXBlocks.COLLECTOR_MK3), key(ProjectEXBlocks.RELAY_MK1),
            key(ProjectEXBlocks.RELAY_MK2), key(ProjectEXBlocks.RELAY_MK3)
            , key(ProjectEXBlocks.CONDENSER_MK1), key(ProjectEXBlocks.CONDENSER_MK2),
            key(ProjectEXBlocks.CONDENSER_MK3), key(ProjectEXBlocks.ALCHEMICAL_CHEST),
            key(ProjectEXBlocks.ADVANCED_ALCHEMICAL_CHEST)
            , key(ProjectEXBlocks.DARK_MATTER_BLOCK), key(ProjectEXBlocks.RED_MATTER_BLOCK),
            key(ProjectEXBlocks.ALCHEMICAL_COAL_BLOCK), key(ProjectEXBlocks.MOBIUS_FUEL_BLOCK),
            key(ProjectEXBlocks.AETERNALIS_FUEL_BLOCK),
            key(ProjectEXBlocks.DARK_MATTER_FURNACE), key(ProjectEXBlocks.RED_MATTER_FURNACE)
            , key(ProjectEXBlocks.DARK_MATTER_PEDESTAL)
        );
        builder(BlockTags.NEEDS_DIAMOND_TOOL).add(expansionMachineKeys());
        builder(BlockTags.NEEDS_DIAMOND_TOOL).add(automationKeys());
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
        builder(ProjectEXTags.DIVINING_ROD_ALLOWED).addTag(BlockTags.MINEABLE_WITH_PICKAXE);
        builder(ProjectEXTags.DIVINING_ROD_DENIED).add(
            key(Blocks.BEDROCK), key(Blocks.BARRIER), key(Blocks.COMMAND_BLOCK),
            key(Blocks.REPEATING_COMMAND_BLOCK), key(Blocks.CHAIN_COMMAND_BLOCK),
            key(Blocks.STRUCTURE_BLOCK), key(Blocks.JIGSAW), key(Blocks.END_PORTAL_FRAME)
        );
        builder(ProjectEXTags.ELEMENTAL_AMULET_ALLOWED).add(
            key(Blocks.AIR), key(Blocks.CAVE_AIR), key(Blocks.VOID_AIR),
            key(Blocks.WATER), key(Blocks.LAVA), key(Blocks.FIRE), key(Blocks.SOUL_FIRE),
            key(Blocks.SHORT_GRASS), key(Blocks.TALL_GRASS), key(Blocks.SNOW)
        );
        builder(ProjectEXTags.ELEMENTAL_AMULET_DENIED).add(
            key(Blocks.BEDROCK), key(Blocks.BARRIER), key(Blocks.COMMAND_BLOCK),
            key(Blocks.REPEATING_COMMAND_BLOCK), key(Blocks.CHAIN_COMMAND_BLOCK),
            key(Blocks.STRUCTURE_BLOCK), key(Blocks.JIGSAW), key(Blocks.END_PORTAL_FRAME)
        );
        builder(ProjectEXTags.DESTRUCTIVE_CATALYST_ALLOWED)
            .addTag(BlockTags.MINEABLE_WITH_PICKAXE)
            .add(key(Blocks.DIRT), key(Blocks.GRASS_BLOCK), key(Blocks.SAND),
                key(Blocks.GRAVEL), key(Blocks.CLAY), key(Blocks.OAK_LOG),
                key(Blocks.SPRUCE_LOG), key(Blocks.BIRCH_LOG), key(Blocks.JUNGLE_LOG),
                key(Blocks.ACACIA_LOG), key(Blocks.DARK_OAK_LOG), key(Blocks.MANGROVE_LOG),
                key(Blocks.CHERRY_LOG));
        builder(ProjectEXTags.DESTRUCTIVE_CATALYST_DENIED).add(
            key(Blocks.BEDROCK), key(Blocks.BARRIER), key(Blocks.COMMAND_BLOCK),
            key(Blocks.REPEATING_COMMAND_BLOCK), key(Blocks.CHAIN_COMMAND_BLOCK),
            key(Blocks.STRUCTURE_BLOCK), key(Blocks.JIGSAW), key(Blocks.END_PORTAL_FRAME),
            key(Blocks.SPAWNER), key(Blocks.VAULT), key(Blocks.TRIAL_SPAWNER)
        );
        builder(ProjectEXTags.INCORRECT_FOR_DARK_MATTER_TOOL).add(
            key(Blocks.BEDROCK), key(Blocks.BARRIER), key(Blocks.COMMAND_BLOCK),
            key(Blocks.REPEATING_COMMAND_BLOCK), key(Blocks.CHAIN_COMMAND_BLOCK),
            key(Blocks.STRUCTURE_BLOCK), key(Blocks.JIGSAW), key(Blocks.END_PORTAL_FRAME)
        );
        builder(ProjectEXTags.INCORRECT_FOR_RED_MATTER_TOOL).add(
            key(Blocks.BEDROCK), key(Blocks.BARRIER), key(Blocks.COMMAND_BLOCK),
            key(Blocks.REPEATING_COMMAND_BLOCK), key(Blocks.CHAIN_COMMAND_BLOCK),
            key(Blocks.STRUCTURE_BLOCK), key(Blocks.JIGSAW), key(Blocks.END_PORTAL_FRAME)
        );
    }

    private static ResourceKey<Block> key(Block block) {
        return BuiltInRegistries.BLOCK.getResourceKey(block).orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static ResourceKey<Block>[] expansionMachineKeys() {
        return java.util.stream.Stream.concat(
            java.util.stream.Stream.of(
                ProjectEXBlocks.EXPANSION_COLLECTORS,
                ProjectEXBlocks.EXPANSION_RELAYS,
                ProjectEXBlocks.POWER_FLOWERS
            ).flatMap(map -> map.values().stream()).map(entry -> key(entry.block())),
            java.util.stream.Stream.of(key(ProjectEXBlocks.COMPACT_SUN))
        ).toArray(ResourceKey[]::new);
    }

    @SuppressWarnings("unchecked")
    private static ResourceKey<Block>[] automationKeys() {
        return java.util.stream.Stream.concat(
            ProjectEXBlocks.EMC_LINKS.values().stream().map(entry -> key(entry.block())),
            java.util.stream.Stream.of(key(ProjectEXBlocks.TRANSMUTATION_INTERFACE))
        ).toArray(ResourceKey[]::new);
    }
}
