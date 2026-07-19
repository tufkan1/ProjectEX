package io.github.tufkan1.projectex.datagen;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.content.ProjectEXBlocks;
import io.github.tufkan1.projectex.content.ProjectEXItems;
import io.github.tufkan1.projectex.content.ProjectEXContentRegistry;
import io.github.tufkan1.projectex.content.recipe.KleinStarUpgradeRecipe;
import io.github.tufkan1.projectex.content.ExpansionMaterialTier;
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
                shaped(RecipeCategory.TOOLS, ProjectEXItems.TRANSMUTATION_TABLET.item())
                    .define('T', ProjectEXBlocks.TRANSMUTATION_TABLE)
                    .define('D', ProjectEXItems.DARK_MATTER.item())
                    .define('S', ProjectEXItems.PHILOSOPHERS_STONE.item())
                    .pattern("DSD").pattern("DTD").pattern("DDD")
                    .unlockedBy("has_dark_matter", has(ProjectEXItems.DARK_MATTER.item()))
                    .save(output, id("transmutation_tablet"));
                shaped(RecipeCategory.TOOLS, ProjectEXItems.REPAIR_TALISMAN.item())
                    .define('L', ProjectEXItems.LOW_COVALENCE_DUST.item())
                    .define('M', ProjectEXItems.MEDIUM_COVALENCE_DUST.item())
                    .define('H', ProjectEXItems.HIGH_COVALENCE_DUST.item())
                    .define('S', Items.STRING).define('P', Items.PAPER)
                    .pattern("LMH").pattern("SPS").pattern("HML")
                    .unlockedBy("has_covalence_dust", has(ProjectEXItems.LOW_COVALENCE_DUST.item()))
                    .save(output, id("repair_talisman"));
                shaped(RecipeCategory.TOOLS, ProjectEXItems.EVERTIDE_AMULET.item())
                    .define('D', ProjectEXItems.DARK_MATTER.item())
                    .define('W', Items.WATER_BUCKET)
                    .pattern("WWW").pattern("DDD").pattern("WWW")
                    .unlockedBy("has_dark_matter", has(ProjectEXItems.DARK_MATTER.item()))
                    .save(output, id("evertide_amulet"));
                shaped(RecipeCategory.TOOLS, ProjectEXItems.VOLCANITE_AMULET.item())
                    .define('D', ProjectEXItems.DARK_MATTER.item())
                    .define('L', Items.LAVA_BUCKET)
                    .pattern("LLL").pattern("DDD").pattern("LLL")
                    .unlockedBy("has_dark_matter", has(ProjectEXItems.DARK_MATTER.item()))
                    .save(output, id("volcanite_amulet"));
                shaped(RecipeCategory.TOOLS, ProjectEXItems.KNOWLEDGE_TOME.item())
                    .define('B', Items.BOOK)
                    .define('L', ProjectEXItems.LOW_COVALENCE_DUST.item())
                    .define('M', ProjectEXItems.MEDIUM_COVALENCE_DUST.item())
                    .define('H', ProjectEXItems.HIGH_COVALENCE_DUST.item())
                    .define('K', ProjectEXItems.KLEIN_STAR_OMEGA.item())
                    .pattern("HML").pattern("KBK").pattern("LMH")
                    .unlockedBy("has_klein_star_omega", has(ProjectEXItems.KLEIN_STAR_OMEGA.item()))
                    .save(output, id("knowledge_tome"));
                shapeless(RecipeCategory.REDSTONE, ProjectEXItems.NOVA_CATALYST.item())
                    .requires(Items.TNT).requires(ProjectEXItems.MOBIUS_FUEL.item())
                    .unlockedBy("has_tnt", has(Items.TNT))
                    .save(output, id("nova_catalyst"));
                shaped(RecipeCategory.TOOLS, ProjectEXItems.DESTRUCTION_CATALYST.item())
                    .define('N', ProjectEXItems.NOVA_CATALYST.item())
                    .define('M', ProjectEXItems.MOBIUS_FUEL.item())
                    .define('F', Items.FLINT_AND_STEEL)
                    .pattern("NMN").pattern("MFM").pattern("NMN")
                    .unlockedBy("has_nova_catalyst", has(ProjectEXItems.NOVA_CATALYST.item()))
                    .save(output, id("destruction_catalyst"));
                shaped(RecipeCategory.TOOLS, ProjectEXItems.BODY_STONE.item())
                    .define('S', Items.SUGAR).define('R', ProjectEXItems.RED_MATTER.item())
                    .define('L', Items.LAPIS_LAZULI)
                    .pattern("SSS").pattern("RLR").pattern("SSS")
                    .unlockedBy("has_red_matter", has(ProjectEXItems.RED_MATTER.item()))
                    .save(output, id("body_stone"));
                shaped(RecipeCategory.TOOLS, ProjectEXItems.SOUL_STONE.item())
                    .define('G', Items.GLOWSTONE_DUST).define('R', ProjectEXItems.RED_MATTER.item())
                    .define('L', Items.LAPIS_LAZULI)
                    .pattern("GGG").pattern("RLR").pattern("GGG")
                    .unlockedBy("has_red_matter", has(ProjectEXItems.RED_MATTER.item()))
                    .save(output, id("soul_stone"));
                shapeless(RecipeCategory.TOOLS, ProjectEXItems.LIFE_STONE.item())
                    .requires(ProjectEXItems.BODY_STONE.item())
                    .requires(ProjectEXItems.SOUL_STONE.item())
                    .unlockedBy("has_body_stone", has(ProjectEXItems.BODY_STONE.item()))
                    .save(output, id("life_stone"));
                shaped(RecipeCategory.MISC, ProjectEXBlocks.DARK_MATTER_PEDESTAL)
                    .define('D', ProjectEXBlocks.DARK_MATTER_BLOCK)
                    .define('R', ProjectEXItems.RED_MATTER.item())
                    .pattern("RDR").pattern("RDR").pattern("DDD")
                    .unlockedBy("has_dark_matter_block", has(ProjectEXBlocks.DARK_MATTER_BLOCK))
                    .save(output, id("dark_matter_pedestal"));
                for (int tier = 0; tier < ProjectEXItems.DIVINING_RODS.size(); tier++) {
                    var rod = ProjectEXItems.DIVINING_RODS.get(tier);
                    var dust = switch (tier) {
                        case 0 -> ProjectEXItems.LOW_COVALENCE_DUST;
                        case 1 -> ProjectEXItems.MEDIUM_COVALENCE_DUST;
                        default -> ProjectEXItems.HIGH_COVALENCE_DUST;
                    };
                    shaped(RecipeCategory.TOOLS, rod.item())
                        .define('D', dust.item())
                        .define('S', tier == 0 ? Items.STICK
                            : ProjectEXItems.DIVINING_RODS.get(tier - 1).item())
                        .pattern("DDD").pattern("DSD").pattern("DDD")
                        .unlockedBy("has_dust", has(dust.item()))
                        .save(output, id("divining_rod_" + (tier + 1)));
                }

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

                net.minecraft.world.level.ItemLike previousFuel = ProjectEXItems.AETERNALIS_FUEL.item();
                net.minecraft.world.level.ItemLike previousMatter = ProjectEXItems.RED_MATTER.item();
                for (ExpansionMaterialTier tier : ExpansionMaterialTier.values()) {
                    var fuel = ProjectEXItems.EXPANSION_FUELS.get(tier.ordinal());
                    var matter = ProjectEXItems.EXPANSION_MATTERS.get(tier.ordinal());
                    shapeless(RecipeCategory.MISC, fuel.item())
                        .requires(previousFuel, 4)
                        .unlockedBy("has_previous_fuel", has(previousFuel))
                        .save(output, id(tier.fuelId()));
                    shaped(RecipeCategory.MISC, matter.item())
                        .define('F', fuel.item()).define('M', previousMatter)
                        .pattern("FMF").pattern("FMF").pattern("FMF")
                        .unlockedBy("has_tier_fuel", has(fuel.item()))
                        .save(output, id(tier.matterId()));
                    previousFuel = fuel.item();
                    previousMatter = matter.item();
                }
                shaped(RecipeCategory.MISC, ProjectEXItems.FADING_MATTER.item())
                    .define('F', previousFuel).define('M', previousMatter)
                    .pattern("FMF").pattern("FMF").pattern("FMF")
                    .unlockedBy("has_white_matter", has(previousMatter))
                    .save(output, id("fading_matter"));
                shaped(RecipeCategory.TOOLS, ProjectEXItems.FINAL_STAR_SHARD.item())
                    .define('S', ProjectEXItems.GARGANTUAN_STAR_OMEGA.item())
                    .define('N', Items.NETHER_STAR)
                    .pattern("SSS").pattern("SNS").pattern("SSS")
                    .unlockedBy("has_gargantuan_omega", has(ProjectEXItems.GARGANTUAN_STAR_OMEGA.item()))
                    .save(output, id("final_star_shard"));
                shaped(RecipeCategory.TOOLS, ProjectEXItems.FINAL_STAR.item())
                    .define('S', ProjectEXItems.FINAL_STAR_SHARD.item())
                    .define('E', Items.DRAGON_EGG)
                    .pattern("SSS").pattern("SES").pattern("SSS")
                    .unlockedBy("has_final_star_shard", has(ProjectEXItems.FINAL_STAR_SHARD.item()))
                    .save(output, id("final_star"));
                shaped(RecipeCategory.FOOD, ProjectEXItems.INFINITE_STEAK.item())
                    .define('S', Items.COOKED_BEEF).define('F', ProjectEXItems.FINAL_STAR_SHARD.item())
                    .pattern("SSS").pattern("SFS").pattern("SSS")
                    .unlockedBy("has_final_star_shard", has(ProjectEXItems.FINAL_STAR_SHARD.item()))
                    .save(output, id("infinite_steak"));

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

                net.minecraft.world.level.ItemLike previousCollector = ProjectEXBlocks.COLLECTOR_MK3;
                net.minecraft.world.level.ItemLike previousRelay = ProjectEXBlocks.RELAY_MK3;
                for (int index = io.github.tufkan1.projectex.machine.ExpansionMachineTier.MAGENTA.ordinal();
                     index < io.github.tufkan1.projectex.machine.ExpansionMachineTier.values().length;
                     index++) {
                    var tier = io.github.tufkan1.projectex.machine.ExpansionMachineTier.values()[index];
                    net.minecraft.world.level.ItemLike matter = index
                        < io.github.tufkan1.projectex.machine.ExpansionMachineTier.FADING.ordinal()
                        ? ProjectEXItems.EXPANSION_MATTERS.get(index
                            - io.github.tufkan1.projectex.machine.ExpansionMachineTier.MAGENTA.ordinal()).item()
                        : tier == io.github.tufkan1.projectex.machine.ExpansionMachineTier.FADING
                            ? ProjectEXItems.FADING_MATTER.item()
                            : ProjectEXItems.FINAL_STAR_SHARD.item();
                    var collector = ProjectEXBlocks.EXPANSION_COLLECTORS.get(tier);
                    var relay = ProjectEXBlocks.EXPANSION_RELAYS.get(tier);
                    shapeless(RecipeCategory.MISC, collector.block())
                        .requires(previousCollector).requires(matter, 8)
                        .unlockedBy("has_previous_collector", has(previousCollector))
                        .save(output, id(tier.id() + "_collector"));
                    shapeless(RecipeCategory.MISC, relay.block())
                        .requires(previousRelay).requires(matter, 8)
                        .unlockedBy("has_previous_relay", has(previousRelay))
                        .save(output, id(tier.id() + "_relay"));
                    previousCollector = collector.block();
                    previousRelay = relay.block();
                }

                for (var tier : io.github.tufkan1.projectex.machine.ExpansionMachineTier.values()) {
                    net.minecraft.world.level.ItemLike collector = switch (tier) {
                        case BASIC -> ProjectEXBlocks.COLLECTOR_MK1;
                        case DARK -> ProjectEXBlocks.COLLECTOR_MK2;
                        case RED -> ProjectEXBlocks.COLLECTOR_MK3;
                        default -> ProjectEXBlocks.EXPANSION_COLLECTORS.get(tier).block();
                    };
                    net.minecraft.world.level.ItemLike relay = switch (tier) {
                        case BASIC -> ProjectEXBlocks.RELAY_MK1;
                        case DARK -> ProjectEXBlocks.RELAY_MK2;
                        case RED -> ProjectEXBlocks.RELAY_MK3;
                        default -> ProjectEXBlocks.EXPANSION_RELAYS.get(tier).block();
                    };
                    var compressed = ProjectEXItems.COMPRESSED_COLLECTORS.get(tier.ordinal());
                    var flower = ProjectEXBlocks.POWER_FLOWERS.get(tier);
                    shaped(RecipeCategory.MISC, compressed.item())
                        .define('C', collector).pattern("CCC").pattern("CCC").pattern("CCC")
                        .unlockedBy("has_tier_collector", has(collector))
                        .save(output, id(tier.id() + "_compressed_collector"));
                    shaped(RecipeCategory.MISC, flower.block())
                        .define('C', compressed.item()).define('L', ProjectEXBlocks.TRANSMUTATION_TABLE)
                        .define('R', relay).pattern("CLC").pattern("RRR").pattern("RRR")
                        .unlockedBy("has_compressed_collector", has(compressed.item()))
                        .save(output, id(tier.id() + "_power_flower"));
                }

                shaped(RecipeCategory.MISC, ProjectEXBlocks.COMPACT_SUN)
                    .define('S', ProjectEXItems.FINAL_STAR.item())
                    .define('H', ProjectEXItems.FINAL_STAR_SHARD.item())
                    .define('Y', ProjectEXItems.EXPANSION_MATTERS.get(
                        ExpansionMaterialTier.YELLOW.ordinal()).item())
                    .pattern("HSH").pattern("SYS").pattern("HSH")
                    .unlockedBy("has_final_star", has(ProjectEXItems.FINAL_STAR.item()))
                    .save(output, id("compact_sun"));

                net.minecraft.world.level.ItemLike previousLink = null;
                for (var tier : io.github.tufkan1.projectex.machine.ExpansionMachineTier.values()) {
                    var link = ProjectEXBlocks.EMC_LINKS.get(tier);
                    net.minecraft.world.level.ItemLike relay = switch (tier) {
                        case BASIC -> ProjectEXBlocks.RELAY_MK1;
                        case DARK -> ProjectEXBlocks.RELAY_MK2;
                        case RED -> ProjectEXBlocks.RELAY_MK3;
                        default -> ProjectEXBlocks.EXPANSION_RELAYS.get(tier).block();
                    };
                    var recipe = shapeless(RecipeCategory.MISC, link.block()).requires(relay);
                    if (previousLink == null) recipe.requires(ProjectEXBlocks.TRANSMUTATION_TABLE);
                    else recipe.requires(previousLink);
                    recipe.unlockedBy("has_tier_relay", has(relay))
                        .save(output, id(tier.id() + "_emc_link"));
                    previousLink = link.block();
                }
                shaped(RecipeCategory.MISC, ProjectEXBlocks.TRANSMUTATION_INTERFACE)
                    .define('L', previousLink)
                    .define('T', ProjectEXBlocks.TRANSMUTATION_TABLE)
                    .define('S', ProjectEXItems.FINAL_STAR_SHARD.item())
                    .pattern("SLS").pattern("LTL").pattern("SLS")
                    .unlockedBy("has_final_emc_link", has(previousLink))
                    .save(output, id("transmutation_interface"));

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
                shaped(RecipeCategory.MISC, ProjectEXBlocks.CONDENSER_MK3)
                    .define('C', ProjectEXBlocks.CONDENSER_MK2)
                    .define('M', ProjectEXItems.EXPANSION_MATTERS.get(0).item())
                    .pattern("MCM").pattern("CCC").pattern("MCM")
                    .unlockedBy("has_condenser_mk2", has(ProjectEXBlocks.CONDENSER_MK2))
                    .save(output, id("condenser_mk3"));

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
