package io.github.tufkan1.projectex.datagen;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.content.ProjectEXBlocks;
import io.github.tufkan1.projectex.content.ProjectEXItems;
import io.github.tufkan1.projectex.content.ProjectEXContentRegistry;
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
import net.minecraft.world.level.block.Blocks;

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

                shaped(RecipeCategory.MISC, ProjectEXBlocks.COLLECTOR_MK1)
                    .define('G', Blocks.GLOWSTONE).define('D', Items.DIAMOND_BLOCK)
                    .define('F', Items.FURNACE).define('L', Items.GLASS)
                    .pattern("LGL").pattern("GFG").pattern("LDL")
                    .unlockedBy("has_diamond_block", has(Items.DIAMOND_BLOCK))
                    .save(output, id("collector_mk1"));
                shaped(RecipeCategory.MISC, ProjectEXBlocks.COLLECTOR_MK2)
                    .define('C', ProjectEXBlocks.COLLECTOR_MK1)
                    .define('D', ProjectEXItems.DARK_MATTER.item()).define('G', Blocks.GLOWSTONE)
                    .pattern("GDG").pattern("DCD").pattern("GDG")
                    .unlockedBy("has_collector_mk1", has(ProjectEXBlocks.COLLECTOR_MK1))
                    .save(output, id("collector_mk2"));
                shaped(RecipeCategory.MISC, ProjectEXBlocks.COLLECTOR_MK3)
                    .define('C', ProjectEXBlocks.COLLECTOR_MK2)
                    .define('R', ProjectEXItems.RED_MATTER.item()).define('G', Blocks.GLOWSTONE)
                    .pattern("GRG").pattern("RCR").pattern("GRG")
                    .unlockedBy("has_collector_mk2", has(ProjectEXBlocks.COLLECTOR_MK2))
                    .save(output, id("collector_mk3"));
                shaped(RecipeCategory.MISC, ProjectEXBlocks.RELAY_MK1)
                    .define('O', Items.OBSIDIAN).define('D', Items.DIAMOND).define('G', Items.GLASS)
                    .pattern("OGO").pattern("DOD").pattern("OGO")
                    .unlockedBy("has_diamond", has(Items.DIAMOND))
                    .save(output, id("relay_mk1"));
                shaped(RecipeCategory.MISC, ProjectEXBlocks.RELAY_MK2)
                    .define('R', ProjectEXBlocks.RELAY_MK1)
                    .define('D', ProjectEXItems.DARK_MATTER.item()).define('O', Items.OBSIDIAN)
                    .pattern("ODO").pattern("DRD").pattern("ODO")
                    .unlockedBy("has_relay_mk1", has(ProjectEXBlocks.RELAY_MK1))
                    .save(output, id("relay_mk2"));
                shaped(RecipeCategory.MISC, ProjectEXBlocks.RELAY_MK3)
                    .define('L', ProjectEXBlocks.RELAY_MK2)
                    .define('R', ProjectEXItems.RED_MATTER.item()).define('O', Items.OBSIDIAN)
                    .pattern("ORO").pattern("RLR").pattern("ORO")
                    .unlockedBy("has_relay_mk2", has(ProjectEXBlocks.RELAY_MK2))
                    .save(output, id("relay_mk3"));

                shaped(RecipeCategory.MISC, ProjectEXBlocks.ALCHEMICAL_CHEST)
                    .define('C', Items.CHEST).define('D', ProjectEXItems.HIGH_COVALENCE_DUST.item())
                    .define('M', ProjectEXItems.DARK_MATTER.item())
                    .pattern("DMD").pattern("MCM").pattern("DMD")
                    .unlockedBy("has_dark_matter", has(ProjectEXItems.DARK_MATTER.item()))
                    .save(output, id("alchemical_chest"));
                shaped(RecipeCategory.MISC, ProjectEXBlocks.CONDENSER_MK1)
                    .define('C', Items.CHEST)
                    .define('D', Items.DIAMOND_BLOCK).define('O', Items.OBSIDIAN)
                    .pattern("ODO").pattern("DCD").pattern("ODO")
                    .unlockedBy("has_alchemical_chest", has(ProjectEXBlocks.ALCHEMICAL_CHEST))
                    .save(output, id("condenser_mk1"));
                shaped(RecipeCategory.MISC, ProjectEXBlocks.CONDENSER_MK2)
                    .define('N', Items.NETHERITE_BLOCK)
                    .define('R', ProjectEXItems.RED_MATTER.item()).define('A', ProjectEXItems.AETERNALIS_FUEL.item())
                    .pattern("ARA").pattern("RNR").pattern("ARA")
                    .unlockedBy("has_condenser_mk1", has(ProjectEXBlocks.CONDENSER_MK1))
                    .save(output, id("condenser_mk2"));

                java.util.List<net.minecraft.world.item.Item> colors = Items.DYE.asList();
                for (int index = 0; index < ProjectEXItems.alchemicalBags().size(); index++) {
                    var bag = ProjectEXItems.alchemicalBags().get(index);
                    shaped(RecipeCategory.TOOLS, bag.item())
                        .define('C', Items.CHEST).define('D', colors.get(index))
                        .define('H', ProjectEXItems.HIGH_COVALENCE_DUST.item())
                        .pattern("HDH").pattern("DCD").pattern("HDH")
                        .unlockedBy("has_high_covalence_dust", has(ProjectEXItems.HIGH_COVALENCE_DUST.item()))
                        .save(output, id(bag.id().getPath()));
                }

                matterBlockRecipes(ProjectEXBlocks.DARK_MATTER_BLOCK, ProjectEXItems.DARK_MATTER.item(), "dark_matter");
                matterBlockRecipes(ProjectEXBlocks.RED_MATTER_BLOCK, ProjectEXItems.RED_MATTER.item(), "red_matter");
                shaped(RecipeCategory.MISC, ProjectEXBlocks.DARK_MATTER_FURNACE)
                    .define('F', Items.FURNACE).define('D', ProjectEXItems.DARK_MATTER.item())
                    .define('O', Items.OBSIDIAN)
                    .pattern("ODO").pattern("DFD").pattern("ODO")
                    .unlockedBy("has_dark_matter", has(ProjectEXItems.DARK_MATTER.item()))
                    .save(output, id("dark_matter_furnace"));
                shaped(RecipeCategory.MISC, ProjectEXBlocks.RED_MATTER_FURNACE)
                    .define('F', ProjectEXBlocks.DARK_MATTER_FURNACE)
                    .define('R', ProjectEXItems.RED_MATTER.item())
                    .define('O', Items.OBSIDIAN)
                    .pattern("ORO").pattern("RFR").pattern("ORO")
                    .unlockedBy("has_dark_matter_furnace", has(ProjectEXBlocks.DARK_MATTER_FURNACE))
                    .save(output, id("red_matter_furnace"));
                matterToolRecipe(ProjectEXItems.DARK_MATTER_PICKAXE, ProjectEXItems.DARK_MATTER.item());
                matterToolRecipe(ProjectEXItems.DARK_MATTER_HAMMER, ProjectEXItems.DARK_MATTER.item());
                matterToolRecipe(ProjectEXItems.RED_MATTER_PICKAXE, ProjectEXItems.RED_MATTER.item());
                matterToolRecipe(ProjectEXItems.RED_MATTER_HAMMER, ProjectEXItems.RED_MATTER.item());
                ProjectEXItems.MATTER_HAND_TOOLS.forEach(entry -> matterToolRecipe(
                    entry, entry.id().getPath().startsWith("red_")
                        ? ProjectEXItems.RED_MATTER.item() : ProjectEXItems.DARK_MATTER.item()
                ));
                ProjectEXItems.MATTER_ARMOR.forEach(entry -> matterArmorRecipe(
                    entry, entry.id().getPath().startsWith("red_")
                        ? ProjectEXItems.RED_MATTER.item() : ProjectEXItems.DARK_MATTER.item()
                ));
            }

            private void matterBlockRecipes(
                net.minecraft.world.level.ItemLike block, net.minecraft.world.level.ItemLike matter, String name
            ) {
                shaped(RecipeCategory.BUILDING_BLOCKS, block)
                    .define('M', matter).pattern("MMM").pattern("MMM").pattern("MMM")
                    .unlockedBy("has_matter", has(matter)).save(output, id(name + "_block"));
                shapeless(RecipeCategory.MISC, matter, 9).requires(block)
                    .unlockedBy("has_block", has(block)).save(output, id(name + "_from_block"));
            }

            private void matterToolRecipe(
                ProjectEXContentRegistry.RegisteredItem<? extends net.minecraft.world.item.Item> entry,
                net.minecraft.world.level.ItemLike matter
            ) {
                String path = entry.id().getPath();
                var recipe = shaped(RecipeCategory.TOOLS, entry.item()).define('M', matter).define('S', Items.STICK);
                if (path.endsWith("pickaxe")) recipe.pattern("MMM").pattern(" S ").pattern(" S ");
                else if (path.endsWith("hammer")) recipe.pattern("MMM").pattern("MSM").pattern(" S ");
                else if (path.endsWith("axe")) recipe.pattern("MM ").pattern("MS ").pattern(" S ");
                else if (path.endsWith("shovel")) recipe.pattern(" M ").pattern(" S ").pattern(" S ");
                else if (path.endsWith("hoe")) recipe.pattern("MM ").pattern(" S ").pattern(" S ");
                else if (path.endsWith("sword")) recipe.pattern(" M ").pattern(" M ").pattern(" S ");
                else throw new IllegalArgumentException("Unknown matter tool: " + path);
                recipe.unlockedBy("has_matter", has(matter)).save(output, id(path));
            }

            private void matterArmorRecipe(
                ProjectEXContentRegistry.RegisteredItem<? extends net.minecraft.world.item.Item> entry,
                net.minecraft.world.level.ItemLike matter
            ) {
                String path = entry.id().getPath();
                var recipe = shaped(RecipeCategory.COMBAT, entry.item()).define('M', matter);
                if (path.endsWith("helmet")) recipe.pattern("MMM").pattern("M M");
                else if (path.endsWith("chestplate")) recipe.pattern("M M").pattern("MMM").pattern("MMM");
                else if (path.endsWith("leggings")) recipe.pattern("MMM").pattern("M M").pattern("M M");
                else if (path.endsWith("boots")) recipe.pattern("M M").pattern("M M");
                else throw new IllegalArgumentException("Unknown matter armor: " + path);
                recipe.unlockedBy("has_matter", has(matter)).save(output, id(path));
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
