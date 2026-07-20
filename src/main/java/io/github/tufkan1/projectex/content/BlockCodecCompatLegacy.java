package io.github.tufkan1.projectex.content;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Function;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Bridges the block codec contract retained by Minecraft 26.2 and 26.3 Snapshot 1. */
abstract class BlockCodecCompat extends BaseEntityBlock {
    private BlockCodecCompat(BlockBehaviour.Properties properties) {
        super(properties);
    }

    static <B extends Block> RecordCodecBuilder<B, BlockBehaviour.Properties> compatPropertiesCodec() {
        return BlockBehaviour.propertiesCodec();
    }

    static <B extends BaseEntityBlock> MapCodec<B> compatSimpleCodec(
        Function<BlockBehaviour.Properties, B> factory
    ) {
        return BlockBehaviour.simpleCodec(factory);
    }
}
