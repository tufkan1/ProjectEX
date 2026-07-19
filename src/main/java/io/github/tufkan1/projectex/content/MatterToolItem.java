package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.api.matter.MatterAreaActionContext;
import io.github.tufkan1.projectex.api.matter.MatterAreaActionProtection;
import io.github.tufkan1.projectex.content.component.MatterToolState;
import io.github.tufkan1.projectex.internal.player.MatterEmcPayment;
import io.github.tufkan1.projectex.matter.MatterActionAuditEvent;
import io.github.tufkan1.projectex.matter.MatterActionPlanner;
import io.github.tufkan1.projectex.matter.MatterTier;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;

/** Chargeable matter pickaxe/hammer with bounded, protection-aware server mining. */
public final class MatterToolItem extends Item {
    public enum Kind { PICKAXE, HAMMER }

    private final MatterTier tier;
    private final Kind kind;

    public MatterToolItem(Properties properties, MatterTier tier, Kind kind) {
        super(properties.stacksTo(1).component(ProjectEXComponents.MATTER_TOOL_STATE, MatterToolState.DEFAULT));
        this.tier = tier;
        this.kind = kind;
    }

    public MatterTier tier() { return tier; }
    public Kind kind() { return kind; }

    @Override @SuppressWarnings("deprecation")
    public void appendHoverText(
        ItemStack stack, TooltipContext context, TooltipDisplay display,
        Consumer<Component> textConsumer, TooltipFlag flags
    ) {
        MatterToolState state = stack.getOrDefault(ProjectEXComponents.MATTER_TOOL_STATE, MatterToolState.DEFAULT);
        textConsumer.accept(Component.translatable(
            "item.projectex.matter_tool.charge", state.charge(), tier.maxCharge()
        ).withStyle(ChatFormatting.GRAY));
        textConsumer.accept(Component.translatable(
            "item.projectex.matter_tool.controls"
        ).withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        MatterToolState current = stack.getOrDefault(ProjectEXComponents.MATTER_TOOL_STATE, MatterToolState.DEFAULT);
        MatterToolState updated = current.next(tier.maxCharge());
        stack.set(ProjectEXComponents.MATTER_TOOL_STATE, updated);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(Component.translatable(
                "item.projectex.matter_tool.charge", updated.charge(), tier.maxCharge()
            ));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        if (!context.isSecondaryUseActive()) return InteractionResult.PASS;
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (!(context.getLevel() instanceof ServerLevel level)
            || !(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.FAIL;
        ItemStack stack = context.getItemInHand();
        if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
        BlockHitResult raycast = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (raycast.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;
        MatterToolState state = stack.getOrDefault(ProjectEXComponents.MATTER_TOOL_STATE, MatterToolState.DEFAULT);
        Result result = mine(level, player, stack, raycast.getBlockPos(), state.charge());
        if (result.committed == 0) return InteractionResult.FAIL;
        player.getCooldowns().addCooldown(stack, tier.actionCooldownTicks());
        return InteractionResult.SUCCESS_SERVER;
    }

    private Result mine(ServerLevel level, ServerPlayer player, ItemStack tool, BlockPos origin, int charge) {
        MatterActionPlanner.Position planOrigin = position(origin);
        List<BlockPos> blocks = cube(origin, charge);
        List<MatterActionPlanner.Position> candidates = blocks.stream()
            .filter(pos -> {
                var state = level.getBlockState(pos);
                return !state.isAir() && state.getDestroySpeed(level, pos) >= 0
                    && tool.isCorrectToolForDrops(state);
            })
            .map(MatterToolItem::position).toList();
        var plan = MatterActionPlanner.plan(
            tier, player.getUUID(), planOrigin, candidates,
            candidate -> {
                BlockPos pos = blockPos(candidate);
                return MatterAreaActionProtection.EVENT.invoker().canAct(new MatterAreaActionContext(
                    level, player, tool, kind.name().toLowerCase(java.util.Locale.ROOT),
                    pos, level.getBlockState(pos)
                ));
            },
            MatterEmcPayment.balance(player), charge
        );
        if (plan.accepted().isEmpty() || !MatterEmcPayment.debit(player, plan.emcCost())) {
            return new Result(0, EmcValue.ZERO);
        }
        int committed = 0;
        for (MatterActionPlanner.Position candidate : plan.accepted()) {
            BlockPos pos = blockPos(candidate);
            if (MatterAreaActionProtection.EVENT.invoker().canAct(new MatterAreaActionContext(
                level, player, tool, kind.name().toLowerCase(java.util.Locale.ROOT),
                pos, level.getBlockState(pos)
            )) && player.gameMode.destroyBlock(pos)) {
                committed++;
            }
        }
        EmcValue spent = tier.emcPerAreaBlock().multiply(committed);
        EmcValue refund = plan.emcCost().subtract(spent);
        if (!refund.equals(EmcValue.ZERO)) MatterEmcPayment.credit(player, refund);
        MatterActionAuditEvent audit = new MatterActionAuditEvent(
            player.getUUID(), tier.id(), kind.name().toLowerCase(java.util.Locale.ROOT),
            candidates.size(), committed, plan.protectionDenied().size(), spent, level.getGameTime()
        );
        ProjectEX.LOGGER.info(
            "Matter action actor={} tier={} action={} attempted={} committed={} denied={} emc={}",
            audit.actor(), audit.tier(), audit.action(), audit.attempted(), audit.committed(),
            audit.protectionDenied(), audit.emcSpent()
        );
        return new Result(committed, spent);
    }

    private static List<BlockPos> cube(BlockPos origin, int radius) {
        List<BlockPos> positions = new ArrayList<>((radius * 2 + 1) * (radius * 2 + 1) * (radius * 2 + 1));
        for (int x = -radius; x <= radius; x++) for (int y = -radius; y <= radius; y++) {
            for (int z = -radius; z <= radius; z++) positions.add(origin.offset(x, y, z));
        }
        return positions;
    }

    private static MatterActionPlanner.Position position(BlockPos pos) {
        return new MatterActionPlanner.Position(pos.getX(), pos.getY(), pos.getZ());
    }
    private static BlockPos blockPos(MatterActionPlanner.Position pos) {
        return new BlockPos(pos.x(), pos.y(), pos.z());
    }
    private record Result(int committed, EmcValue spent) { }
}
