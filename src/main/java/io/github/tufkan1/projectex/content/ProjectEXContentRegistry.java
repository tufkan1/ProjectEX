package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.ProjectEX;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Typed registration helpers shared by ProjectEX content families. */
public final class ProjectEXContentRegistry {
    private ProjectEXContentRegistry() {
    }

    public static RegisteredBlock registerBlockWithItem(
        String path,
        Function<BlockBehaviour.Properties, Block> factory,
        BlockBehaviour.Properties properties
    ) {
        Identifier id = ProjectEX.id(path);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        Block block = factory.apply(properties.setId(blockKey));
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        BlockItem item = new BlockItem(
            block,
            new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()
        );

        Registry.register(BuiltInRegistries.ITEM, itemKey, item);
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
        return new RegisteredBlock(id, block, item);
    }

    public static <T extends Item> RegisteredItem<T> registerItem(
        String path,
        Function<Item.Properties, T> factory,
        Item.Properties properties
    ) {
        Identifier id = ProjectEX.id(path);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        T item = factory.apply(properties.setId(itemKey));
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);
        return new RegisteredItem<>(id, item);
    }

    /** Stable identifiers and typed values used by runtime code and datagen. */
    public record RegisteredBlock(Identifier id, Block block, BlockItem item) {
    }

    public record RegisteredItem<T extends Item>(Identifier id, T item) {
    }
}
