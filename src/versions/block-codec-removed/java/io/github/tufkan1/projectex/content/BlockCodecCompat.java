package io.github.tufkan1.projectex.content;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Function;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Retired codec shim for 26.3 Snapshot 2+, where block codecs are no longer queried. */
final class BlockCodecCompat {
    private BlockCodecCompat() {
    }

    static <B extends Block> RecordCodecBuilder<B, BlockBehaviour.Properties> compatPropertiesCodec() {
        return null;
    }

    static <B extends BaseEntityBlock> MapCodec<B> compatSimpleCodec(
        Function<BlockBehaviour.Properties, B> factory
    ) {
        return null;
    }
}
