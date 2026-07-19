package io.github.tufkan1.projectex.content.recipe;

import com.mojang.serialization.MapCodec;
import io.github.tufkan1.projectex.ProjectEX;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

/** Custom recipes whose runtime logic cannot be represented by vanilla JSON alone. */
public final class ProjectEXRecipeSerializers {
    public static final RecipeSerializer<KleinStarUpgradeRecipe> KLEIN_STAR_UPGRADE =
        Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            ProjectEX.id("klein_star_upgrade"),
            new RecipeSerializer<>(
                MapCodec.unit(KleinStarUpgradeRecipe::new),
                StreamCodec.unit(new KleinStarUpgradeRecipe())
            )
        );

    private ProjectEXRecipeSerializers() {
    }

    public static void register() {
        // Class loading performs registration.
    }
}
