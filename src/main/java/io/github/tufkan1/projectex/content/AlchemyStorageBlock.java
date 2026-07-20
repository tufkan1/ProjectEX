package io.github.tufkan1.projectex.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tufkan1.projectex.content.storage.AlchemyStorageBlockEntity;
import io.github.tufkan1.projectex.storage.StorageKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Server-authoritative shell shared by condensers and the alchemical chest. */
public final class AlchemyStorageBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final MapCodec<AlchemyStorageBlock> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            BlockCodecCompat.compatPropertiesCodec(),
            Codec.STRING.xmap(StorageKind::valueOf, StorageKind::name).fieldOf("kind")
                .forGetter(AlchemyStorageBlock::kind)
        ).apply(instance, AlchemyStorageBlock::new)
    );

    private final StorageKind kind;

    public AlchemyStorageBlock(BlockBehaviour.Properties properties, StorageKind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public StorageKind kind() { return kind; }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlchemyStorageBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> type
    ) {
        return level.isClientSide() || !kind.condenser() ? null
            : createTickerHelper(type, ProjectEXBlockEntities.ALCHEMY_STORAGE,
                AlchemyStorageBlockEntity::tickServer);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof ServerPlayer player
            && level.getBlockEntity(pos) instanceof AlchemyStorageBlockEntity storage) {
            storage.claim(player.getUUID());
        }
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
        InteractionHand hand, BlockHitResult hit
    ) {
        if (kind != StorageKind.ADVANCED_ALCHEMICAL_CHEST || !player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(pos) instanceof AlchemyStorageBlockEntity storage)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.FAIL;
        if (!storage.toggleAdvancedFilter(stack, player)) return InteractionResult.FAIL;
        serverPlayer.sendOverlayMessage(Component.translatable(
            "block.projectex.advanced_alchemical_chest.filter_item",
            stack.getHoverName(), storage.storageState().advancedConfig().itemIds().size()
        ));
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit
    ) {
        if (!(level.getBlockEntity(pos) instanceof AlchemyStorageBlockEntity storage)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer) || !storage.canUse(player)) {
            return InteractionResult.FAIL;
        }
        if (kind == StorageKind.ADVANCED_ALCHEMICAL_CHEST && player.isSecondaryUseActive()) {
            if (!storage.cycleAdvancedFilter(player)) return InteractionResult.FAIL;
            serverPlayer.sendOverlayMessage(Component.translatable(
                "block.projectex.advanced_alchemical_chest.filter_mode",
                Component.translatable("block.projectex.advanced_alchemical_chest.filter_mode."
                    + storage.storageState().advancedConfig().filterMode().name().toLowerCase(java.util.Locale.ROOT))
            ));
            return InteractionResult.SUCCESS_SERVER;
        }
        storage.sortAdvanced();
        serverPlayer.openMenu(storage);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }

    @Override
    protected int getAnalogOutputSignal(
        BlockState state, Level level, BlockPos pos, net.minecraft.core.Direction direction
    ) {
        return level.getBlockEntity(pos) instanceof AlchemyStorageBlockEntity storage
            ? storage.comparatorSignal() : 0;
    }
}
