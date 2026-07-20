package io.github.tufkan1.projectex.content;

import com.mojang.serialization.MapCodec;
import io.github.tufkan1.projectex.content.pedestal.DarkMatterPedestalBlockEntity;
import io.github.tufkan1.projectex.machine.MachineRedstoneMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
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

/** Owner-controlled Dark Matter Pedestal; attack extracts, use toggles, sneak-use configures. */
public final class DarkMatterPedestalBlock extends BaseEntityBlock {
    public static final MapCodec<DarkMatterPedestalBlock> CODEC =
        BlockCodecCompat.compatSimpleCodec(DarkMatterPedestalBlock::new);

    public DarkMatterPedestalBlock(BlockBehaviour.Properties properties) { super(properties); }
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DarkMatterPedestalBlockEntity(pos, state);
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type,
            ProjectEXBlockEntities.DARK_MATTER_PEDESTAL,
            DarkMatterPedestalBlockEntity::tickServer);
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                                      LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof ServerPlayer player
            && level.getBlockEntity(pos) instanceof DarkMatterPedestalBlockEntity pedestal) {
            pedestal.claim(player.getUUID());
        }
    }

    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
        BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof DarkMatterPedestalBlockEntity pedestal))
            return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        return pedestal.insert(stack, player) ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                          Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof DarkMatterPedestalBlockEntity pedestal))
            return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        boolean changed = player.isSecondaryUseActive()
            ? pedestal.cycleRedstoneMode(player) : pedestal.toggleActive(player);
        if (changed && player instanceof ServerPlayer serverPlayer) {
            Component feedback = player.isSecondaryUseActive()
                ? Component.translatable("block.projectex.dark_matter_pedestal.redstone",
                    Component.translatable("screen.projectex.machine_redstone."
                        + redstoneKey(pedestal.redstoneMode())))
                : Component.translatable(pedestal.active()
                    ? "block.projectex.dark_matter_pedestal.active"
                    : "block.projectex.dark_matter_pedestal.inactive");
            serverPlayer.sendOverlayMessage(feedback);
        }
        return changed ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
    }

    @Override protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide()
            && level.getBlockEntity(pos) instanceof DarkMatterPedestalBlockEntity pedestal) {
            ItemStack extracted = pedestal.extract(player);
            if (!extracted.isEmpty() && !player.getInventory().add(extracted)) player.drop(extracted, false);
        }
    }

    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()
            && level.getBlockEntity(pos) instanceof DarkMatterPedestalBlockEntity pedestal) {
            ItemStack extracted = pedestal.ejectOnBreak();
            if (!extracted.isEmpty()) net.minecraft.world.Containers.dropItemStack(
                level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, extracted);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos,
                                                   Direction direction) {
        return level.getBlockEntity(pos) instanceof DarkMatterPedestalBlockEntity pedestal
            ? pedestal.comparatorSignal() : 0;
    }

    private static String redstoneKey(MachineRedstoneMode mode) {
        return switch (mode) {
            case IGNORED -> "ignored";
            case REQUIRE_SIGNAL -> "require_signal";
            case REQUIRE_NO_SIGNAL -> "require_no_signal";
        };
    }
}
