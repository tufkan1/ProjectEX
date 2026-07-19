package io.github.tufkan1.projectex.content.alchemy;

import io.github.tufkan1.projectex.api.alchemy.WorldTransmutationContext;
import io.github.tufkan1.projectex.api.alchemy.WorldTransmutationProtection;
import io.github.tufkan1.projectex.content.ProjectEXTags;
import io.github.tufkan1.projectex.content.component.ActiveItemMode;
import io.github.tufkan1.projectex.content.component.ActiveItemState;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Plans, validates, and atomically commits Philosopher's Stone world changes. */
public final class WorldTransmutationService {
    private static final List<Block> CYCLE = List.of(
        Blocks.STONE,
        Blocks.COBBLESTONE,
        Blocks.GRAVEL,
        Blocks.SAND,
        Blocks.GRASS_BLOCK,
        Blocks.DIRT
    );
    private static final Map<Block, Block> FORWARD = cycleMap(false);
    private static final Map<Block, Block> REVERSE = cycleMap(true);

    private WorldTransmutationService() {
    }

    public static boolean hasTransformation(BlockState state) {
        return FORWARD.containsKey(state.getBlock());
    }

    public static BlockState targetFor(BlockState original, boolean reverse) {
        Block target = (reverse ? REVERSE : FORWARD).get(original.getBlock());
        return target == null ? null : target.defaultBlockState();
    }

    public static Result transform(
        ServerLevel level,
        ServerPlayer player,
        ItemStack catalyst,
        BlockPos origin,
        Direction clickedFace,
        Direction horizontalDirection,
        ActiveItemState activeState,
        boolean reverse
    ) {
        LinkedHashMap<BlockPos, Change> plan = new LinkedHashMap<>();
        for (BlockPos position : targets(origin, clickedFace, horizontalDirection, activeState)) {
            BlockState original = level.getBlockState(position);
            BlockState target = targetFor(original, reverse);
            if (target == null) {
                continue;
            }
            if (!isAllowed(level, player, catalyst, position, original, target, clickedFace)) {
                return Result.denied();
            }
            plan.put(position, new Change(original, target));
        }

        if (plan.isEmpty()) {
            return Result.unsupported();
        }

        List<Map.Entry<BlockPos, Change>> committed = new ArrayList<>();
        for (Map.Entry<BlockPos, Change> entry : plan.entrySet()) {
            if (!level.setBlock(entry.getKey(), entry.getValue().target(), Block.UPDATE_ALL)) {
                rollback(level, committed);
                return Result.rolledBack();
            }
            committed.add(entry);
        }
        return Result.changed(committed.size());
    }

    private static boolean isAllowed(
        ServerLevel level,
        ServerPlayer player,
        ItemStack catalyst,
        BlockPos position,
        BlockState original,
        BlockState target,
        Direction clickedFace
    ) {
        if (!level.hasChunk(position.getX() >> 4, position.getZ() >> 4)
            || !original.is(ProjectEXTags.PHILOSOPHERS_STONE_ALLOWED)
            || original.is(ProjectEXTags.PHILOSOPHERS_STONE_DENIED)
            || level.getBlockEntity(position) != null
            || !target.canSurvive(level, position)
            || !level.mayInteract(player, position)
            || !player.mayUseItemAt(position, clickedFace, catalyst)) {
            return false;
        }
        return WorldTransmutationProtection.EVENT.invoker().canTransform(
            new WorldTransmutationContext(
                level,
                player,
                catalyst,
                position.immutable(),
                original,
                target
            )
        );
    }

    private static List<BlockPos> targets(
        BlockPos origin,
        Direction face,
        Direction horizontalDirection,
        ActiveItemState state
    ) {
        int charge = state.charge();
        List<BlockPos> positions = new ArrayList<>();
        for (int x = -charge; x <= charge; x++) {
            for (int y = -charge; y <= charge; y++) {
                for (int z = -charge; z <= charge; z++) {
                    if (inShape(state.mode(), face, horizontalDirection, x, y, z)) {
                        positions.add(origin.offset(x, y, z).immutable());
                    }
                }
            }
        }
        return positions;
    }

    private static boolean inShape(
        ActiveItemMode mode,
        Direction face,
        Direction horizontalDirection,
        int x,
        int y,
        int z
    ) {
        return switch (mode) {
            case CUBE -> true;
            case PANEL -> switch (face.getAxis()) {
                case X -> x == 0;
                case Y -> y == 0;
                case Z -> z == 0;
            };
            case LINE -> switch (horizontalDirection.getAxis()) {
                case X -> y == 0 && z == 0;
                case Z -> x == 0 && y == 0;
                case Y -> x == 0 && z == 0;
            };
        };
    }

    private static void rollback(
        ServerLevel level,
        List<Map.Entry<BlockPos, Change>> committed
    ) {
        for (int index = committed.size() - 1; index >= 0; index--) {
            Map.Entry<BlockPos, Change> entry = committed.get(index);
            level.setBlock(entry.getKey(), entry.getValue().original(), Block.UPDATE_ALL);
        }
    }

    private static Map<Block, Block> cycleMap(boolean reverse) {
        LinkedHashMap<Block, Block> result = new LinkedHashMap<>();
        for (int index = 0; index < CYCLE.size(); index++) {
            int offset = reverse ? -1 : 1;
            result.put(CYCLE.get(index), CYCLE.get(Math.floorMod(index + offset, CYCLE.size())));
        }
        return Map.copyOf(result);
    }

    private record Change(BlockState original, BlockState target) {
    }

    public record Result(Status status, int changedBlocks) {
        public static Result changed(int count) {
            return new Result(Status.CHANGED, count);
        }

        public static Result denied() {
            return new Result(Status.DENIED, 0);
        }

        public static Result unsupported() {
            return new Result(Status.UNSUPPORTED, 0);
        }

        public static Result rolledBack() {
            return new Result(Status.ROLLED_BACK, 0);
        }
    }

    public enum Status {
        CHANGED,
        DENIED,
        UNSUPPORTED,
        ROLLED_BACK
    }
}
