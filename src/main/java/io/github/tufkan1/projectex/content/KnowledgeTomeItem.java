package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.internal.player.PlayerAlchemySavedData;
import io.github.tufkan1.projectex.player.PlayerAlchemyState;
import java.util.TreeSet;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Atomically teaches every item-only entry in one immutable server EMC snapshot. */
public final class KnowledgeTomeItem extends Item {
    private static final int COOLDOWN_TICKS = 20;

    public KnowledgeTomeItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)
            || !KnowledgeTomePolicy.mode().permits(serverPlayer)) {
            return InteractionResult.FAIL;
        }
        ItemStack tome = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(tome)) return InteractionResult.FAIL;
        TreeSet<EmcKey> complete = ProjectEX.emc().snapshot().values().entrySet().stream()
            .filter(entry -> entry.getKey().componentsJson() == null
                && !entry.getValue().equals(io.github.tufkan1.projectex.api.emc.EmcValue.ZERO))
            .map(entry -> entry.getKey().item())
            .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        if (complete.size() > PlayerAlchemyState.MAX_KNOWLEDGE_ENTRIES) {
            ProjectEX.LOGGER.error("Knowledge Tome snapshot has {} entries, exceeding safe cap {}",
                complete.size(), PlayerAlchemyState.MAX_KNOWLEDGE_ENTRIES);
            return InteractionResult.FAIL;
        }
        PlayerAlchemySavedData data = PlayerAlchemySavedData.get(level.getServer());
        PlayerAlchemyState before = data.state(player.getUUID());
        complete.addAll(before.knowledge());
        if (complete.equals(before.knowledge())) return InteractionResult.FAIL;
        PlayerAlchemyState after = new PlayerAlchemyState(before.balance(), complete);
        if (!data.compareAndSet(player.getUUID(), before, after)) return InteractionResult.FAIL;
        int learned = after.knowledge().size() - before.knowledge().size();
        if (!player.getAbilities().instabuild) tome.shrink(1);
        player.getCooldowns().addCooldown(tome, COOLDOWN_TICKS);
        serverPlayer.sendSystemMessage(Component.translatable(
            "item.projectex.knowledge_tome.learned", learned, after.knowledge().size()));
        return InteractionResult.SUCCESS_SERVER;
    }
}
