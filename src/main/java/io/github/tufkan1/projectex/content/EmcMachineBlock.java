package io.github.tufkan1.projectex.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tufkan1.projectex.content.machine.EmcMachineBlockEntity;
import io.github.tufkan1.projectex.machine.MachineTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Shared server-owned block shell for baseline collectors and relays. */
public final class EmcMachineBlock extends BaseEntityBlock {
    public static final MapCodec<EmcMachineBlock> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            BlockCodecCompat.compatPropertiesCodec(),
            Codec.STRING.fieldOf("tier")
                .xmap(MachineTier::valueOf, MachineTier::name)
                .forGetter(EmcMachineBlock::tier)
        ).apply(instance, EmcMachineBlock::new)
    );

    private final MachineTier tier;

    public EmcMachineBlock(BlockBehaviour.Properties properties, MachineTier tier) {
        super(properties);
        this.tier = tier;
    }

    public MachineTier tier() {
        return tier;
    }

    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EmcMachineBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level,
        BlockState state,
        BlockEntityType<T> type
    ) {
        return level.isClientSide()
            ? null
            : createTickerHelper(
                type,
                ProjectEXBlockEntities.EMC_MACHINE,
                EmcMachineBlockEntity::tickServer
            );
    }

    @Override
    public void setPlacedBy(
        Level level,
        BlockPos pos,
        BlockState state,
        LivingEntity placer,
        ItemStack stack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof ServerPlayer player
            && level.getBlockEntity(pos) instanceof EmcMachineBlockEntity machine) {
            machine.claim(player.getUUID());
        }
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        BlockHitResult hit
    ) {
        if (!(level.getBlockEntity(pos) instanceof EmcMachineBlockEntity machine)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!machine.canUse(player) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }
        serverPlayer.openMenu(machine);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(
        BlockState state,
        Level level,
        BlockPos pos,
        Direction direction
    ) {
        return level.getBlockEntity(pos) instanceof EmcMachineBlockEntity machine
            ? machine.comparatorSignal()
            : 0;
    }
}
