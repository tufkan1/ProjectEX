package io.github.tufkan1.projectex.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tufkan1.projectex.content.automation.AutomationBlockEntity;
import io.github.tufkan1.projectex.content.automation.AutomationBlockKind;
import io.github.tufkan1.projectex.machine.ExpansionMachineTier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

/** Claimed server-owned shell for EMC Links and the Transmutation Interface. */
public final class AutomationBlock extends BaseEntityBlock {
    public static final MapCodec<AutomationBlock> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            BlockCodecCompat.compatPropertiesCodec(),
            Codec.STRING.xmap(AutomationBlockKind::valueOf, AutomationBlockKind::name)
                .fieldOf("kind").forGetter(AutomationBlock::kind),
            Codec.STRING.xmap(ExpansionMachineTier::valueOf, ExpansionMachineTier::name)
                .fieldOf("tier").forGetter(AutomationBlock::tier)
        ).apply(instance, AutomationBlock::new)
    );

    private final AutomationBlockKind kind;
    private final ExpansionMachineTier tier;

    public AutomationBlock(BlockBehaviour.Properties properties, AutomationBlockKind kind,
                           ExpansionMachineTier tier) {
        super(properties);
        this.kind = kind;
        this.tier = tier;
    }

    public AutomationBlockKind kind() { return kind; }
    public ExpansionMachineTier tier() { return tier; }

    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AutomationBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer,
                            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof ServerPlayer player
            && level.getBlockEntity(pos) instanceof AutomationBlockEntity automation) {
            automation.placedBy(player);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit
    ) {
        if (!(level.getBlockEntity(pos) instanceof AutomationBlockEntity automation)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer) || !automation.canUse(player)) {
            return InteractionResult.FAIL;
        }
        serverPlayer.sendOverlayMessage(automation.getDisplayName());
        return InteractionResult.SUCCESS_SERVER;
    }
}
