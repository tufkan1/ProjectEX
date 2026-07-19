package io.github.tufkan1.projectex.content;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Common registration for ProjectEX blocks and their block items. */
public final class ProjectEXBlocks {
    public static final ProjectEXContentRegistry.RegisteredBlock TRANSMUTATION_TABLE_FAMILY =
        ProjectEXContentRegistry.registerBlockWithItem(
        "transmutation_table",
        TransmutationTableBlock::new,
        BlockBehaviour.Properties.of().strength(5.0F, 1_200.0F)
            .requiresCorrectToolForDrops().sound(SoundType.STONE)
    );
    public static final Block TRANSMUTATION_TABLE = TRANSMUTATION_TABLE_FAMILY.block();

    private ProjectEXBlocks() {
    }

    public static void register() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
            .register(entries -> entries.accept(TRANSMUTATION_TABLE.asItem()));
    }

}
