package io.github.tufkan1.projectex.content.recipe;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.content.KleinStarItem;
import io.github.tufkan1.projectex.content.KleinStarTier;
import io.github.tufkan1.projectex.content.ProjectEXComponents;
import io.github.tufkan1.projectex.content.ProjectEXItems;
import io.github.tufkan1.projectex.content.component.PortableEmcState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** Upgrades four equal-tier stars while preserving their exact combined EMC. */
public final class KleinStarUpgradeRecipe extends CustomRecipe {
    private static final int REQUIRED_STARS = 4;

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return plan(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        UpgradePlan plan = plan(input);
        if (plan == null) {
            return ItemStack.EMPTY;
        }
        ItemStack result = new ItemStack(ProjectEXItems.kleinStars().get(plan.target().ordinal()).item());
        result.set(
            ProjectEXComponents.PORTABLE_EMC,
            new PortableEmcState(PortableEmcState.CURRENT_VERSION, plan.stored())
        );
        return result;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return ProjectEXRecipeSerializers.KLEIN_STAR_UPGRADE;
    }

    private static UpgradePlan plan(CraftingInput input) {
        KleinStarTier source = null;
        int stars = 0;
        int catalysts = 0;
        EmcValue stored = EmcValue.ZERO;
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof KleinStarItem star) {
                if (source != null && source != star.tier()) {
                    return null;
                }
                source = star.tier();
                stars++;
                stored = stored.add(stack.getOrDefault(
                    ProjectEXComponents.PORTABLE_EMC,
                    PortableEmcState.EMPTY
                ).stored());
            } else if (stack.is(ProjectEXItems.AETERNALIS_FUEL.item())) {
                catalysts++;
            } else {
                return null;
            }
        }

        if (source == null || source.next() == null
            || stars != REQUIRED_STARS || catalysts != 1
            || stored.compareTo(source.next().capacity()) > 0) {
            return null;
        }
        return new UpgradePlan(source.next(), stored);
    }

    private record UpgradePlan(KleinStarTier target, EmcValue stored) {
    }
}
