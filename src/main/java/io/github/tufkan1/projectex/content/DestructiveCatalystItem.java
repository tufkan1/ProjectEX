package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.api.utility.UtilityWorldActionContext;
import io.github.tufkan1.projectex.api.utility.UtilityWorldActionProtection;
import io.github.tufkan1.projectex.content.component.ActiveItemMode;
import io.github.tufkan1.projectex.content.component.ActiveItemState;
import io.github.tufkan1.projectex.internal.player.MatterEmcPayment;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Bounded one-shot Nova or reusable EMC-backed directional destruction utility. */
public final class DestructiveCatalystItem extends ChargeableUtilityItem {
    public enum Kind { NOVA, DESTRUCTION }

    public static final EmcValue EMC_PER_BLOCK = EmcValue.of(8);
    public static final int MAXIMUM_TARGETS = 72;
    private static final int COOLDOWN_TICKS = 10;
    private final Kind kind;

    public DestructiveCatalystItem(Item.Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override protected int maximumCharge(ItemStack stack) {
        return kind == Kind.NOVA ? 0 : ActiveItemState.MAX_CHARGE;
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        Level rawLevel = context.getLevel();
        if (rawLevel.isClientSide()) return InteractionResult.SUCCESS;
        if (!DestructiveCatalystPolicy.enabled()
            || !(rawLevel instanceof ServerLevel level)
            || !(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.FAIL;
        }
        ItemStack catalyst = context.getItemInHand();
        if (player.getCooldowns().isOnCooldown(catalyst)) return InteractionResult.FAIL;
        List<BlockPos> planned = targets(context).stream()
            .filter(pos -> eligible(level, player, catalyst, pos, context.getClickedFace()))
            .limit(MAXIMUM_TARGETS).toList();
        if (planned.isEmpty()) return InteractionResult.FAIL;

        int payable = planned.size();
        if (kind == Kind.DESTRUCTION && !player.getAbilities().instabuild) {
            BigInteger affordable = MatterEmcPayment.balance(player).amount()
                .divide(EMC_PER_BLOCK.amount());
            payable = Math.min(payable, affordable.min(BigInteger.valueOf(MAXIMUM_TARGETS)).intValue());
            if (payable == 0) return InteractionResult.FAIL;
            if (!MatterEmcPayment.debit(player, EMC_PER_BLOCK.multiply(payable))) {
                return InteractionResult.FAIL;
            }
        }

        int committed = 0;
        for (BlockPos pos : planned.subList(0, payable)) {
            if (eligible(level, player, catalyst, pos, context.getClickedFace())
                && player.gameMode.destroyBlock(pos)) committed++;
        }
        if (kind == Kind.DESTRUCTION && !player.getAbilities().instabuild && committed < payable) {
            MatterEmcPayment.credit(player, EMC_PER_BLOCK.multiply(payable - committed));
        }
        if (committed == 0) return InteractionResult.FAIL;
        if (kind == Kind.NOVA && !player.getAbilities().instabuild) catalyst.shrink(1);
        player.getCooldowns().addCooldown(catalyst, COOLDOWN_TICKS);
        level.playSound(null, context.getClickedPos(), SoundEvents.GENERIC_EXPLODE.value(),
            SoundSource.PLAYERS, 0.7F, 1.2F);
        player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
            "item.projectex.destructive_catalyst.result", committed,
            kind == Kind.DESTRUCTION && !player.getAbilities().instabuild
                ? EMC_PER_BLOCK.multiply(committed).amount() : BigInteger.ZERO));
        return InteractionResult.SUCCESS_SERVER;
    }

    private boolean eligible(ServerLevel level, ServerPlayer player, ItemStack catalyst,
        BlockPos pos, Direction clickedFace) {
        if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) return false;
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !state.is(ProjectEXTags.DESTRUCTIVE_CATALYST_ALLOWED)
            || state.is(ProjectEXTags.DESTRUCTIVE_CATALYST_DENIED)
            || state.getDestroySpeed(level, pos) < 0 || state.getDestroySpeed(level, pos) >= 50
            || level.getBlockEntity(pos) != null || !level.mayInteract(player, pos)
            || !player.mayUseItemAt(pos, clickedFace, catalyst)) return false;
        return UtilityWorldActionProtection.EVENT.invoker().canAct(new UtilityWorldActionContext(
            level, player, catalyst, kind == Kind.NOVA
                ? "nova_catalyst_destroy" : "destruction_catalyst_destroy",
            pos.immutable(), state));
    }

    private List<BlockPos> targets(UseOnContext context) {
        BlockPos origin = context.getClickedPos();
        if (kind == Kind.NOVA) {
            List<BlockPos> result = new ArrayList<>(27);
            for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) result.add(origin.offset(x, y, z).immutable());
            }
            return result;
        }
        ActiveItemState active = context.getItemInHand().getOrDefault(
            ProjectEXComponents.ACTIVE_ITEM_STATE, ActiveItemState.DEFAULT);
        int depth = switch (active.charge()) { case 0 -> 1; case 1 -> 4; default -> 8; };
        List<BlockPos> result = new ArrayList<>(MAXIMUM_TARGETS);
        Direction inward = context.getClickedFace().getOpposite();
        for (int step = 0; step < depth; step++) {
            BlockPos center = origin.relative(inward, step);
            int radiusA = active.mode() == ActiveItemMode.LINE ? 0 : 1;
            int radiusB = active.mode() == ActiveItemMode.CUBE ? 1 : 0;
            for (int a = -radiusA; a <= radiusA; a++) for (int b = -radiusB; b <= radiusB; b++) {
                result.add(orthogonal(center, inward, a, b).immutable());
            }
        }
        return result;
    }

    private static BlockPos orthogonal(BlockPos center, Direction direction, int a, int b) {
        return switch (direction.getAxis()) {
            case X -> center.offset(0, a, b);
            case Y -> center.offset(a, 0, b);
            case Z -> center.offset(a, b, 0);
        };
    }
}
