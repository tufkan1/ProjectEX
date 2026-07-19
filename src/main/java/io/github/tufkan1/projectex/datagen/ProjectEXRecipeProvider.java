package io.github.tufkan1.projectex.datagen;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.content.ProjectEXBlocks;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;

/** Crafting recipes for the core ProjectEX content families. */
public final class ProjectEXRecipeProvider extends FabricRecipeProvider {
    public ProjectEXRecipeProvider(
        FabricPackOutput output,
        CompletableFuture<HolderLookup.Provider> registriesFuture
    ) {
        super(output, registriesFuture);
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        return new RecipeProvider(registries, output) {
            @Override
            public void buildRecipes() {
                ResourceKey<Recipe<?>> id = ResourceKey.create(
                    Registries.RECIPE,
                    ProjectEX.id("transmutation_table")
                );
                shaped(RecipeCategory.MISC, ProjectEXBlocks.TRANSMUTATION_TABLE)
                    .define('D', Items.DIAMOND)
                    .define('O', Items.OBSIDIAN)
                    .define('S', Items.STONE)
                    .pattern("SDS")
                    .pattern("OOO")
                    .pattern("SSS")
                    .unlockedBy("has_diamond", has(Items.DIAMOND))
                    .save(output, id);
            }
        };
    }

    @Override
    public String getName() {
        return "ProjectEX recipes";
    }
}
