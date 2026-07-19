package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.api.utility.UtilityWorldActionContext;
import io.github.tufkan1.projectex.api.utility.UtilityWorldActionProtection;
import io.github.tufkan1.projectex.content.component.ActiveItemState;
import io.github.tufkan1.projectex.api.fabric.MinecraftEmcAdapter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Bounded 3x3 inward EMC scan that never loads chunks and honors tags and claim callbacks. */
public final class DiviningRodItem extends ChargeableUtilityItem {
    private final int maximumCharge;

    public DiviningRodItem(Properties properties, int maximumCharge) {
        super(properties);
        this.maximumCharge = Math.clamp(maximumCharge, 0, ActiveItemState.MAX_CHARGE);
    }

    @Override protected int maximumCharge(ItemStack stack) { return maximumCharge; }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)
            || !(context.getPlayer() instanceof ServerPlayer player)) {
            return context.getLevel().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        ScanResult result = scan(level, player, context.getItemInHand(), context.getClickedPos(),
            context.getClickedFace());
        if (result.scannedBlocks() == 0) return InteractionResult.FAIL;
        player.sendSystemMessage(Component.translatable(
            "item.projectex.divining_rod.result", result.scannedBlocks(), result.average().amount()));
        for (int index = 0; index < result.highest().size(); index++) {
            player.sendSystemMessage(Component.translatable(
                "item.projectex.divining_rod.highest", index + 1, result.highest().get(index).amount()));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    public static ScanResult scan(
        ServerLevel level,
        ServerPlayer player,
        ItemStack rod,
        BlockPos origin,
        net.minecraft.core.Direction face
    ) {
        ActiveItemState state = rod.getOrDefault(ProjectEXComponents.ACTIVE_ITEM_STATE,
            ActiveItemState.DEFAULT);
        int maximum = rod.getItem() instanceof DiviningRodItem item ? item.maximumCharge : 0;
        int charge = Math.min(state.charge(), maximum);
        int depth = switch (charge) { case 0 -> 3; case 1 -> 16; default -> 64; };
        BigInteger total = BigInteger.ZERO;
        List<EmcValue> unique = new ArrayList<>();
        int scanned = 0;
        BlockPos inward = origin.relative(face.getOpposite());
        for (int step = 0; step < depth; step++) {
            BlockPos center = inward.relative(face.getOpposite(), step);
            for (int first = -1; first <= 1; first++) {
                for (int second = -1; second <= 1; second++) {
                    BlockPos pos = orthogonal(center, face, first, second);
                    if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) continue;
                    BlockState blockState = level.getBlockState(pos);
                    if (blockState.isAir() || !blockState.is(ProjectEXTags.DIVINING_ROD_ALLOWED)
                        || blockState.is(ProjectEXTags.DIVINING_ROD_DENIED)
                        || !UtilityWorldActionProtection.EVENT.invoker().canAct(
                            new UtilityWorldActionContext(level, player, rod, "divining_rod_scan",
                                pos.immutable(), blockState))) continue;
                    List<ItemStack> drops = Block.getDrops(blockState, level, pos,
                        level.getBlockEntity(pos), player, rod);
                    ItemStack representative = drops.stream().filter(drop -> !drop.isEmpty())
                        .findFirst().orElseGet(() -> new ItemStack(blockState.getBlock().asItem()));
                    EmcValue value = valueOf(level, representative);
                    if (value.equals(EmcValue.ZERO) && !representative.isEmpty()) {
                        var input = new net.minecraft.world.item.crafting.SingleRecipeInput(representative);
                        value = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level)
                            .map(holder -> valueOf(level, holder.value().assemble(input)))
                            .orElse(EmcValue.ZERO);
                    }
                    total = total.add(value.amount());
                    scanned++;
                    if (!value.equals(EmcValue.ZERO) && !unique.contains(value)) unique.add(value);
                }
            }
        }
        unique.sort(Comparator.reverseOrder());
        int reported = maximum == 0 ? 0 : maximum == 1 ? 1 : 3;
        return new ScanResult(scanned,
            scanned == 0 ? EmcValue.ZERO : new EmcValue(total.divide(BigInteger.valueOf(scanned))),
            unique.stream().limit(reported).toList());
    }

    private static BlockPos orthogonal(BlockPos center, net.minecraft.core.Direction face, int a, int b) {
        return switch (face.getAxis()) {
            case X -> center.offset(0, a, b);
            case Y -> center.offset(a, 0, b);
            case Z -> center.offset(a, b, 0);
        };
    }

    private static EmcValue valueOf(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) return EmcValue.ZERO;
        return MinecraftEmcAdapter.exactMatch(stack, level.registryAccess())
            .flatMap(ProjectEX.emc()::find).orElse(EmcValue.ZERO);
    }

    public record ScanResult(int scannedBlocks, EmcValue average, List<EmcValue> highest) {
        public ScanResult {
            if (scannedBlocks < 0) throw new IllegalArgumentException("Negative scan count");
            java.util.Objects.requireNonNull(average, "average");
            highest = List.copyOf(highest);
        }
    }
}
