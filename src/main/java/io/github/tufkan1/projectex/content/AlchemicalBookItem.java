package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.content.component.AlchemicalBookState;
import io.github.tufkan1.projectex.network.AlchemicalBookNetworking;
import io.github.tufkan1.projectex.teleport.AlchemicalBookTier;
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

/** Portable EMC-powered destination book with optional owner-bound storage. */
public final class AlchemicalBookItem extends Item {
    private final AlchemicalBookTier tier;

    public AlchemicalBookItem(Properties properties, AlchemicalBookTier tier) {
        super(properties.stacksTo(1).component(ProjectEXComponents.ALCHEMICAL_BOOK_STATE,
            AlchemicalBookState.EMPTY));
        this.tier = tier;
    }

    public AlchemicalBookTier tier() { return tier; }

    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        ItemStack stack = player.getItemInHand(hand);
        AlchemicalBookState state = stack.getOrDefault(
            ProjectEXComponents.ALCHEMICAL_BOOK_STATE, AlchemicalBookState.EMPTY);
        if (player.isSecondaryUseActive()) {
            if (!tier.bindable()) {
                serverPlayer.sendOverlayMessage(Component.translatable(
                    "item.projectex.alchemical_book.not_bindable"));
                return InteractionResult.FAIL;
            }
            if (state.owner().isEmpty()) {
                stack.set(ProjectEXComponents.ALCHEMICAL_BOOK_STATE, state.bind(player.getUUID()));
                serverPlayer.sendSystemMessage(Component.translatable(
                    "item.projectex.alchemical_book.bound"));
                return InteractionResult.SUCCESS_SERVER;
            }
            if (!state.owner().orElseThrow().equals(player.getUUID())) {
                serverPlayer.sendSystemMessage(Component.translatable(
                    "item.projectex.alchemical_book.not_owner", state.owner().orElseThrow().toString()));
                return InteractionResult.FAIL;
            }
            stack.set(ProjectEXComponents.ALCHEMICAL_BOOK_STATE, state.unbind());
            serverPlayer.sendSystemMessage(Component.translatable(
                "item.projectex.alchemical_book.unbound"));
            return InteractionResult.SUCCESS_SERVER;
        }
        AlchemicalBookNetworking.open(serverPlayer, hand, stack, tier);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override public boolean isFoil(ItemStack stack) {
        return stack.getOrDefault(ProjectEXComponents.ALCHEMICAL_BOOK_STATE,
            AlchemicalBookState.EMPTY).owner().isPresent();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(
        ItemStack stack, TooltipContext context, TooltipDisplay display,
        Consumer<Component> textConsumer, TooltipFlag flags
    ) {
        textConsumer.accept(Component.translatable("item.projectex.alchemical_book.tooltip")
            .withStyle(ChatFormatting.GRAY));
        textConsumer.accept(Component.translatable("item.projectex.alchemical_book.tier."
            + tier.name().toLowerCase(java.util.Locale.ROOT), tier.emcPerBlock())
            .withStyle(ChatFormatting.RED));
        AlchemicalBookState state = stack.getOrDefault(
            ProjectEXComponents.ALCHEMICAL_BOOK_STATE, AlchemicalBookState.EMPTY);
        state.owner().ifPresent(owner -> textConsumer.accept(Component.translatable(
            "item.projectex.alchemical_book.owner", owner.toString()).withStyle(ChatFormatting.AQUA)));
    }
}
