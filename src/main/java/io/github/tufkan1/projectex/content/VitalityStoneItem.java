package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.content.pedestal.PedestalEffectItem;
import io.github.tufkan1.projectex.internal.player.MatterEmcPayment;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Exact-cost Body, Soul, and combined Life Stone utility and bounded pedestal effect. */
public final class VitalityStoneItem extends Item implements PedestalEffectItem {
    public enum Kind { BODY, SOUL, LIFE }

    public static final EmcValue COST_PER_EFFECT = EmcValue.of(64);
    private static final int COOLDOWN_TICKS = 20;
    private final Kind kind;

    public VitalityStoneItem(Properties properties, Kind kind) {
        super(properties.stacksTo(1));
        this.kind = kind;
    }

    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.FAIL;
        ItemStack stone = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stone)) return InteractionResult.FAIL;
        boolean feed = kind != Kind.SOUL && player.getFoodData().needsFood();
        boolean heal = kind != Kind.BODY && player.getHealth() < player.getMaxHealth();
        int effects = (feed ? 1 : 0) + (heal ? 1 : 0);
        if (effects == 0) return InteractionResult.FAIL;
        EmcValue cost = COST_PER_EFFECT.multiply(effects);
        if (!player.getAbilities().instabuild && !MatterEmcPayment.debit(serverPlayer, cost)) {
            return InteractionResult.FAIL;
        }
        if (feed) player.getFoodData().eat(2, 10.0F);
        if (heal) player.heal(2.0F);
        player.getCooldowns().addCooldown(stone, COOLDOWN_TICKS);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
            SoundSource.PLAYERS, 0.5F, 1.5F);
        serverPlayer.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
            "item.projectex.vitality_stone.result", cost.amount()));
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override public int pedestalIntervalTicks() { return 20; }

    @Override public void applyPedestalEffect(ServerLevel level, BlockPos pedestalPos,
        ItemStack stack, int range, int maximumPlayers) {
        AABB bounds = new AABB(pedestalPos).inflate(range);
        level.getPlayers(player -> player.isAlive() && !player.isSpectator()
                && bounds.contains(player.position())
                && level.getChunkSource().hasChunk(player.getBlockX() >> 4, player.getBlockZ() >> 4))
            .stream().sorted(Comparator.comparing(Player::getUUID)).limit(maximumPlayers)
            .forEach(player -> {
                if (kind != Kind.SOUL && player.getFoodData().needsFood()) {
                    player.getFoodData().eat(1, 1.0F);
                }
                if (kind != Kind.BODY && player.getHealth() < player.getMaxHealth()) {
                    player.heal(1.0F);
                }
            });
    }
}
