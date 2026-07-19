package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.content.component.KnowledgeBookState;
import io.github.tufkan1.projectex.internal.knowledge.KnowledgeSharingRuntime;
import io.github.tufkan1.projectex.knowledge.KnowledgeShareWorkflow;
import io.github.tufkan1.projectex.knowledge.KnowledgeSharingConfig;
import io.github.tufkan1.projectex.knowledge.KnowledgeSnapshot;
import io.github.tufkan1.projectex.network.KnowledgeSharingNetworking;
import java.time.Instant;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/** Signed, owner-bound sharing book with a mandatory server preview and explicit confirmation. */
public final class KnowledgeSharingBookItem extends Item {
    private static final int COOLDOWN_TICKS = 10;
    public KnowledgeSharingBookItem(Properties properties) { super(properties.stacksTo(1)); }

    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
        KnowledgeBookState state = stack.get(ProjectEXComponents.KNOWLEDGE_BOOK_STATE);
        if (state == null) {
            if (!player.isSecondaryUseActive()) {
                serverPlayer.sendOverlayMessage(Component.translatable(
                    "item.projectex.knowledge_sharing_book.capture_hint"));
                return InteractionResult.FAIL;
            }
            return capture(serverPlayer, stack, null);
        }
        if (player.isSecondaryUseActive()) {
            if (!state.snapshot().ownerId().equals(player.getUUID())) {
                serverPlayer.sendOverlayMessage(Component.translatable(
                    "item.projectex.knowledge_sharing_book.owner_only"));
                return InteractionResult.FAIL;
            }
            return capture(serverPlayer, stack, state);
        }
        if (state.snapshot().ownerId().equals(player.getUUID())) {
            KnowledgeBookState updated = state.nextMode();
            stack.set(ProjectEXComponents.KNOWLEDGE_BOOK_STATE, updated);
            player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
            serverPlayer.sendOverlayMessage(Component.translatable(
                "item.projectex.knowledge_sharing_book.mode",
                Component.translatable("item.projectex.knowledge_sharing_book.mode."
                    + updated.mode().name().toLowerCase(java.util.Locale.ROOT))));
            return InteractionResult.SUCCESS_SERVER;
        }
        boolean opened = KnowledgeSharingNetworking.preview(serverPlayer, state);
        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        return opened ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
    }

    @Override public boolean isFoil(ItemStack stack) {
        return stack.has(ProjectEXComponents.KNOWLEDGE_BOOK_STATE);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(
        ItemStack stack, TooltipContext context, TooltipDisplay display,
        Consumer<Component> textConsumer, TooltipFlag flags
    ) {
        KnowledgeBookState state = stack.get(ProjectEXComponents.KNOWLEDGE_BOOK_STATE);
        if (state == null) {
            textConsumer.accept(Component.translatable(
                "item.projectex.knowledge_sharing_book.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        textConsumer.accept(Component.translatable("item.projectex.knowledge_sharing_book.owner",
            state.snapshot().ownerId().toString()).withStyle(ChatFormatting.AQUA));
        textConsumer.accept(Component.translatable("item.projectex.knowledge_sharing_book.entries",
            state.snapshot().knowledge().size()).withStyle(ChatFormatting.GRAY));
        textConsumer.accept(Component.translatable("item.projectex.knowledge_sharing_book.mode",
            Component.translatable("item.projectex.knowledge_sharing_book.mode."
                + state.mode().name().toLowerCase(java.util.Locale.ROOT))).withStyle(ChatFormatting.GRAY));
    }

    private static InteractionResult capture(
        ServerPlayer player, ItemStack stack, KnowledgeBookState previous
    ) {
        KnowledgeShareWorkflow.SharingPolicy policy = KnowledgeSharingConfig.snapshot().policy();
        if (policy == KnowledgeShareWorkflow.SharingPolicy.DISABLED
            || policy == KnowledgeShareWorkflow.SharingPolicy.CREATIVE_ONLY
                && !player.getAbilities().instabuild) {
            player.sendOverlayMessage(Component.translatable("item.projectex.knowledge_sharing_book.disabled"));
            return InteractionResult.FAIL;
        }
        try {
            KnowledgeSnapshot snapshot = KnowledgeSharingRuntime.get(player.level().getServer())
                .capture(player, Instant.now());
            KnowledgeShareWorkflow.Mode mode = previous == null
                ? KnowledgeShareWorkflow.Mode.MERGE : previous.mode();
            stack.set(ProjectEXComponents.KNOWLEDGE_BOOK_STATE,
                new KnowledgeBookState(KnowledgeBookState.CURRENT_VERSION, snapshot, mode));
            player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
            player.sendOverlayMessage(Component.translatable(
                "item.projectex.knowledge_sharing_book.stored", snapshot.knowledge().size()));
            return InteractionResult.SUCCESS_SERVER;
        } catch (IllegalArgumentException exception) {
            player.sendOverlayMessage(Component.translatable(
                "item.projectex.knowledge_sharing_book.oversized"));
            return InteractionResult.FAIL;
        }
    }
}
