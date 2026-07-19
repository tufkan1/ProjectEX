package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.content.component.PortableEmcState;
import java.math.BigInteger;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** Read-only client presentation for a server-authoritative Klein Star balance. */
public final class KleinStarItem extends Item {
    private static final int BAR_WIDTH = 13;
    private final KleinStarTier tier;

    public KleinStarItem(Properties properties, KleinStarTier tier) {
        super(properties.stacksTo(1).component(
            ProjectEXComponents.PORTABLE_EMC,
            PortableEmcState.EMPTY
        ));
        this.tier = tier;
    }

    public KleinStarTier tier() {
        return tier;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stored(stack).signum() > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        BigInteger stored = stored(stack);
        return stored.multiply(BigInteger.valueOf(BAR_WIDTH))
            .divide(tier.capacity().amount())
            .min(BigInteger.valueOf(BAR_WIDTH))
            .intValue();
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x7F3FFF;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(
        ItemStack stack,
        TooltipContext context,
        TooltipDisplay display,
        Consumer<Component> textConsumer,
        TooltipFlag flags
    ) {
        textConsumer.accept(Component.translatable(
            "item.projectex.klein_star.stored",
            stored(stack).toString(),
            tier.capacity().amount().toString()
        ).withStyle(ChatFormatting.GRAY));
    }

    private static BigInteger stored(ItemStack stack) {
        return stack.getOrDefault(ProjectEXComponents.PORTABLE_EMC, PortableEmcState.EMPTY)
            .stored().amount();
    }
}
