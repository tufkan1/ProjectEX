package io.github.tufkan1.projectex.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tufkan1.projectex.content.matter.MatterFurnaceBlockEntity;
import io.github.tufkan1.projectex.matter.MatterTier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Server-ticked dark/red matter furnace shell. */
public final class MatterFurnaceBlock extends BaseEntityBlock {
    public static final MapCodec<MatterFurnaceBlock> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            BlockCodecCompat.compatPropertiesCodec(),
            Codec.STRING.xmap(MatterFurnaceBlock::tier, MatterTier::id)
                .fieldOf("tier").forGetter(MatterFurnaceBlock::tier)
        ).apply(instance, MatterFurnaceBlock::new)
    );

    private final MatterTier tier;

    public MatterFurnaceBlock(BlockBehaviour.Properties properties, MatterTier tier) {
        super(properties);
        this.tier = tier;
    }

    public MatterTier tier() { return tier; }

    private static MatterTier tier(String id) {
        MatterTier tier = MatterTier.DEFAULTS.get(id);
        if (tier == null) throw new IllegalArgumentException("Unknown matter tier: " + id);
        return tier;
    }

    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MatterFurnaceBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> type
    ) {
        return level.isClientSide() ? null : createTickerHelper(
            type, ProjectEXBlockEntities.MATTER_FURNACE, MatterFurnaceBlockEntity::tickServer
        );
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit
    ) {
        if (!(level.getBlockEntity(pos) instanceof MatterFurnaceBlockEntity furnace)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.FAIL;
        serverPlayer.openMenu(furnace);
        return InteractionResult.SUCCESS_SERVER;
    }
}
