package io.github.tufkan1.projectex.emc.mapping.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.emc.mapping.EmcIngredient;
import io.github.tufkan1.projectex.emc.mapping.EmcRecipe;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MinecraftRecipeAdapterTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void adaptsCountsAndOutputAmountFromARealCraftingRecipe() {
        RecipeHolder<ShapelessRecipe> holder = holder("planks", new ItemStackTemplate(Items.OAK_PLANKS, 4),
            List.of(Ingredient.of(Items.OAK_LOG), Ingredient.of(Items.OAK_LOG)));

        EmcRecipe recipe = MinecraftRecipeAdapter.adaptOne(holder).orElseThrow();

        assertEquals(key("projectex:planks"), recipe.id());
        assertEquals(key("minecraft:oak_planks"), recipe.output());
        assertEquals(4, recipe.outputCount());
        assertEquals(List.of(
            EmcIngredient.of(key("minecraft:oak_log"), 1),
            EmcIngredient.of(key("minecraft:oak_log"), 1)
        ), recipe.inputs());
        assertTrue(recipe.remainders().isEmpty());
    }

    @Test
    void recordsTheCommonCraftingRemainder() {
        RecipeHolder<ShapelessRecipe> holder = holder("wet_item", new ItemStackTemplate(Items.CLAY_BALL),
            List.of(Ingredient.of(Items.WATER_BUCKET)));

        ItemStackTemplate remainder = Items.WATER_BUCKET.getCraftingRemainder();
        assertNotNull(remainder);
        assertTrue(remainder.components().isEmpty(), remainder.toString());
        assertTrue(remainder.item().unwrapKey().isPresent(), remainder.toString());
        EmcRecipe recipe = MinecraftRecipeAdapter.adaptOne(holder).orElseThrow();

        assertEquals(List.of(EmcIngredient.of(key("minecraft:bucket"), 1)), recipe.remainders());
    }

    private static RecipeHolder<ShapelessRecipe> holder(
        String id,
        ItemStackTemplate result,
        List<Ingredient> ingredients
    ) {
        ShapelessRecipe recipe = new ShapelessRecipe(
            new Recipe.CommonInfo(false),
            new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, ""),
            result,
            ingredients
        );
        ResourceKey<Recipe<?>> key = ResourceKey.create(
            Registries.RECIPE,
            Identifier.fromNamespaceAndPath("projectex", id)
        );
        return new RecipeHolder<>(key, recipe);
    }

    private static EmcKey key(String value) {
        return EmcKey.parse(value);
    }
}
