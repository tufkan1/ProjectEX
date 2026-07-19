package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.ProjectEX;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Common registration for ProjectEX blocks and their block items. */
public final class ProjectEXBlocks {
    public static final Block TRANSMUTATION_TABLE = register(
        "transmutation_table",
        TransmutationTableBlock::new,
        BlockBehaviour.Properties.of().strength(5.0F, 1_200.0F)
            .requiresCorrectToolForDrops().sound(SoundType.STONE)
    );

    private ProjectEXBlocks() {
    }

    public static void register() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
            .register(entries -> entries.accept(TRANSMUTATION_TABLE.asItem()));
    }

    private static Block register(
        String path,
        Function<BlockBehaviour.Properties, Block> factory,
        BlockBehaviour.Properties properties
    ) {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, ProjectEX.id(path));
        Block block = factory.apply(properties.setId(blockKey));
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, ProjectEX.id(path));
        Registry.register(BuiltInRegistries.ITEM, itemKey,
            new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
        return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
    }
}
