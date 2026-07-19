package io.github.tufkan1.projectex.datagen;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.content.ProjectEXBlocks;
import io.github.tufkan1.projectex.content.ProjectEXItems;
import io.github.tufkan1.projectex.content.recipe.KleinStarUpgradeRecipe;
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
                shaped(RecipeCategory.MISC, ProjectEXBlocks.TRANSMUTATION_TABLE)
                    .define('D', Items.DIAMOND)
                    .define('O', Items.OBSIDIAN)
                    .define('S', Items.STONE)
                    .pattern("SDS")
                    .pattern("OOO")
                    .pattern("SSS")
                    .unlockedBy("has_diamond", has(Items.DIAMOND))
                    .save(output, id("transmutation_table"));

                shaped(RecipeCategory.TOOLS, ProjectEXItems.PHILOSOPHERS_STONE.item())
                    .define('D', Items.DIAMOND)
                    .define('G', Items.GLOWSTONE_DUST)
                    .define('R', Items.REDSTONE)
                    .pattern("RGR")
                    .pattern("GDG")
                    .pattern("RGR")
                    .unlockedBy("has_glowstone", has(Items.GLOWSTONE_DUST))
                    .save(output, id("philosophers_stone"));

                shapeless(RecipeCategory.MISC, ProjectEXItems.LOW_COVALENCE_DUST.item(), 40)
                    .requires(Items.CHARCOAL)
                    .requires(Items.COBBLESTONE, 8)
                    .unlockedBy("has_cobblestone", has(Items.COBBLESTONE))
                    .save(output, id("low_covalence_dust"));
                shapeless(RecipeCategory.MISC, ProjectEXItems.MEDIUM_COVALENCE_DUST.item(), 40)
                    .requires(Items.IRON_INGOT)
                    .requires(Items.REDSTONE)
                    .unlockedBy("has_redstone", has(Items.REDSTONE))
                    .save(output, id("medium_covalence_dust"));
                shapeless(RecipeCategory.MISC, ProjectEXItems.HIGH_COVALENCE_DUST.item(), 40)
                    .requires(Items.DIAMOND)
                    .requires(Items.COAL)
                    .unlockedBy("has_diamond", has(Items.DIAMOND))
                    .save(output, id("high_covalence_dust"));

                shapeless(RecipeCategory.MISC, ProjectEXItems.ALCHEMICAL_COAL.item())
                    .requires(Items.COAL, 4)
                    .requires(Items.REDSTONE)
                    .unlockedBy("has_redstone", has(Items.REDSTONE))
                    .save(output, id("alchemical_coal"));
                shapeless(RecipeCategory.MISC, ProjectEXItems.MOBIUS_FUEL.item())
                    .requires(ProjectEXItems.ALCHEMICAL_COAL.item(), 4)
                    .requires(Items.GLOWSTONE_DUST)
                    .unlockedBy("has_alchemical_coal", has(ProjectEXItems.ALCHEMICAL_COAL.item()))
                    .save(output, id("mobius_fuel"));
                shapeless(RecipeCategory.MISC, ProjectEXItems.AETERNALIS_FUEL.item())
                    .requires(ProjectEXItems.MOBIUS_FUEL.item(), 4)
                    .requires(Items.DIAMOND)
                    .unlockedBy("has_mobius_fuel", has(ProjectEXItems.MOBIUS_FUEL.item()))
                    .save(output, id("aeternalis_fuel"));

                shaped(RecipeCategory.MISC, ProjectEXItems.DARK_MATTER.item())
                    .define('A', ProjectEXItems.AETERNALIS_FUEL.item())
                    .define('D', Items.DIAMOND_BLOCK)
                    .pattern("AAA")
                    .pattern("ADA")
                    .pattern("AAA")
                    .unlockedBy("has_aeternalis_fuel", has(ProjectEXItems.AETERNALIS_FUEL.item()))
                    .save(output, id("dark_matter"));
                shaped(RecipeCategory.MISC, ProjectEXItems.RED_MATTER.item())
                    .define('A', ProjectEXItems.AETERNALIS_FUEL.item())
                    .define('D', ProjectEXItems.DARK_MATTER.item())
                    .pattern("DDD")
                    .pattern("DAD")
                    .pattern("DDD")
                    .unlockedBy("has_dark_matter", has(ProjectEXItems.DARK_MATTER.item()))
                    .save(output, id("red_matter"));

                shaped(RecipeCategory.TOOLS, ProjectEXItems.KLEIN_STAR_EIN.item())
                    .define('A', ProjectEXItems.AETERNALIS_FUEL.item())
                    .define('D', ProjectEXItems.DARK_MATTER.item())
                    .pattern(" A ")
                    .pattern("ADA")
                    .pattern(" A ")
                    .unlockedBy("has_dark_matter", has(ProjectEXItems.DARK_MATTER.item()))
                    .save(output, id("klein_star_ein"));
                output.accept(
                    id("klein_star_upgrade"),
                    new KleinStarUpgradeRecipe(),
                    null
                );
            }

            private ResourceKey<Recipe<?>> id(String path) {
                return ResourceKey.create(Registries.RECIPE, ProjectEX.id(path));
            }
        };
    }

    @Override
    public String getName() {
        return "ProjectEX recipes";
    }
}
