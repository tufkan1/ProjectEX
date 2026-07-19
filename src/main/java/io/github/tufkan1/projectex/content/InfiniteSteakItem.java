package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.endgame.EndgameRuntimeConfig;
import io.github.tufkan1.projectex.endgame.FinalStarAccess;
import io.github.tufkan1.projectex.internal.player.MatterEmcPayment;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** Non-depleting food with atomic server EMC payment or a Final Star lease. */
public final class InfiniteSteakItem extends Item {
    public InfiniteSteakItem(Properties properties) {
        super(properties.stacksTo(1).food(Foods.COOKED_BEEF));
    }

    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!EndgameRuntimeConfig.snapshot().infiniteConsumablesEnabled()
            || !player.canEat(false) || player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player) || level.isClientSide()) return stack;
        var config = EndgameRuntimeConfig.snapshot();
        if (!config.infiniteConsumablesEnabled() || !player.canEat(false)
            || player.getCooldowns().isOnCooldown(stack)) return stack;
        var star = FinalStarAccess.find(player);
        boolean paid = star.filter(io.github.tufkan1.projectex.api.endgame.FinalStarCapability::ready)
            .map(io.github.tufkan1.projectex.api.endgame.FinalStarCapability::activate)
            .orElseGet(() -> MatterEmcPayment.debit(player, config.infiniteSteakCost()));
        if (!paid) return stack;
        player.getFoodData().eat(Foods.COOKED_BEEF);
        player.getCooldowns().addCooldown(stack, config.infiniteSteakCooldownTicks());
        return stack;
    }

    @Override @SuppressWarnings("deprecation")
    public void appendHoverText(
        ItemStack stack, TooltipContext context, TooltipDisplay display,
        Consumer<Component> textConsumer, TooltipFlag flags
    ) {
        textConsumer.accept(Component.translatable(
            "item.projectex.infinite_steak.tooltip",
            EndgameRuntimeConfig.snapshot().infiniteSteakCost().amount().toString()
        ).withStyle(ChatFormatting.GRAY));
    }
}
