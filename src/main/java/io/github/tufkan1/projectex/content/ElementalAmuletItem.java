package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.api.utility.UtilityWorldActionContext;
import io.github.tufkan1.projectex.api.utility.UtilityWorldActionProtection;
import io.github.tufkan1.projectex.content.pedestal.PedestalEffectItem;
import io.github.tufkan1.projectex.internal.player.MatterEmcPayment;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** Infinite water or EMC-backed lava utility with protected world and pedestal effects. */
public final class ElementalAmuletItem extends Item implements PedestalEffectItem {
    public enum Element { WATER, LAVA }

    public static final EmcValue LAVA_COST = EmcValue.of(32);
    private static final int USE_COOLDOWN_TICKS = 4;
    private static final int PEDESTAL_INTERVAL_TICKS = 1_200;
    private static final int WEATHER_DURATION_TICKS = 12_000;
    private final Element element;

    public ElementalAmuletItem(Properties properties, Element element) {
        super(properties.stacksTo(1));
        this.element = element;
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        Level rawLevel = context.getLevel();
        if (rawLevel.isClientSide()) return InteractionResult.SUCCESS;
        if (!(rawLevel instanceof ServerLevel level)
            || !(context.getPlayer() instanceof ServerPlayer player)
            || player.getCooldowns().isOnCooldown(context.getItemInHand())) {
            return InteractionResult.FAIL;
        }
        BlockPos clicked = context.getClickedPos();
        BlockPos target = level.getBlockState(clicked).canBeReplaced()
            ? clicked : clicked.relative(context.getClickedFace());
        BlockState original = level.getBlockState(target);
        ItemStack amulet = context.getItemInHand();
        if (!canPlace(level, player, amulet, target, original, context)) {
            return InteractionResult.FAIL;
        }
        boolean paid = element == Element.WATER || player.getAbilities().instabuild
            || MatterEmcPayment.debit(player, LAVA_COST);
        if (!paid) return InteractionResult.FAIL;
        BlockState replacement = block().defaultBlockState();
        if (!level.setBlock(target, replacement, Block.UPDATE_ALL)) {
            if (element == Element.LAVA && !player.getAbilities().instabuild) {
                MatterEmcPayment.credit(player, LAVA_COST);
            }
            return InteractionResult.FAIL;
        }
        level.playSound(null, target, element == Element.WATER
            ? SoundEvents.BUCKET_EMPTY : SoundEvents.BUCKET_EMPTY_LAVA,
            SoundSource.BLOCKS, 1.0F, 1.0F);
        player.getCooldowns().addCooldown(amulet, USE_COOLDOWN_TICKS);
        return InteractionResult.SUCCESS_SERVER;
    }

    private boolean canPlace(ServerLevel level, ServerPlayer player, ItemStack amulet,
        BlockPos target, BlockState original, UseOnContext context) {
        Fluid fluid = element == Element.WATER ? Fluids.WATER : Fluids.LAVA;
        if (!level.getChunkSource().hasChunk(target.getX() >> 4, target.getZ() >> 4)
            || (element == Element.WATER && level.dimension() == Level.NETHER)
            || !original.canBeReplaced(fluid)
            || !original.is(ProjectEXTags.ELEMENTAL_AMULET_ALLOWED)
            || original.is(ProjectEXTags.ELEMENTAL_AMULET_DENIED)
            || level.getBlockEntity(target) != null
            || !level.mayInteract(player, target)
            || !player.mayUseItemAt(target, context.getClickedFace(), amulet)) {
            return false;
        }
        return UtilityWorldActionProtection.EVENT.invoker().canAct(
            new UtilityWorldActionContext(level, player, amulet,
                element == Element.WATER ? "evertide_place_water" : "volcanite_place_lava",
                target.immutable(), original));
    }

    private Block block() { return element == Element.WATER ? Blocks.WATER : Blocks.LAVA; }

    @Override public int pedestalIntervalTicks() { return PEDESTAL_INTERVAL_TICKS; }

    @Override public void applyPedestalEffect(ServerLevel level, BlockPos pedestalPos,
        ItemStack stack, int range, int maximumPlayers) {
        var weather = level.getWeatherData();
        if (element == Element.WATER) {
            weather.setClearWeatherTime(0);
            weather.setRainTime(WEATHER_DURATION_TICKS);
            weather.setThunderTime(WEATHER_DURATION_TICKS);
            weather.setRaining(true);
            weather.setThundering(false);
        } else {
            weather.setClearWeatherTime(WEATHER_DURATION_TICKS);
            weather.setRainTime(0);
            weather.setThunderTime(0);
            weather.setRaining(false);
            weather.setThundering(false);
        }
        weather.setDirty();
    }
}
