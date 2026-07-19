package io.github.tufkan1.projectex.emc.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RecipeEmcMapperTest {
    private static final EmcKey LOG = key("minecraft:oak_log");
    private static final EmcKey PLANKS = key("minecraft:oak_planks");
    private static final EmcKey STICK = key("minecraft:stick");

    @Test
    void mapsCountsAndRoundsUpOutputDivision() {
        EmcRecipe planks = EmcRecipe.of(key("minecraft:oak_planks"),
            List.of(EmcIngredient.of(LOG, 1)), PLANKS, 4);

        EmcMappingResult result = RecipeEmcMapper.map(Map.of(LOG, EmcValue.of(33)), List.of(planks));

        assertEquals(EmcValue.of(9), result.values().get(PLANKS));
        assertEquals(planks.id(), result.derivations().get(PLANKS).recipe());
    }

    @Test
    void choosesTheCheapestKnownTagAlternativeDeterministically() {
        EmcKey birch = key("minecraft:birch_planks");
        EmcIngredient planksTag = new EmcIngredient(List.of(PLANKS, birch), 2);
        EmcRecipe sticks = EmcRecipe.of(key("minecraft:sticks"), List.of(planksTag), STICK, 4);

        EmcMappingResult result = RecipeEmcMapper.map(
            Map.of(PLANKS, EmcValue.of(8), birch, EmcValue.of(6)), List.of(sticks));

        assertEquals(EmcValue.of(3), result.values().get(STICK));
        assertEquals(Map.of(birch, 2), result.derivations().get(STICK).chosenInputs());
    }

    @Test
    void subtractsKnownContainerRemainders() {
        EmcKey bucket = key("minecraft:bucket");
        EmcKey waterBucket = key("minecraft:water_bucket");
        EmcKey wetDust = key("example:wet_dust");
        EmcRecipe recipe = new EmcRecipe(key("example:wet_dust_recipe"),
            List.of(EmcIngredient.of(waterBucket, 1)), wetDust, 1,
            List.of(EmcIngredient.of(bucket, 1)), false);

        EmcMappingResult result = RecipeEmcMapper.map(
            Map.of(waterBucket, EmcValue.of(800), bucket, EmcValue.of(768)), List.of(recipe));

        assertEquals(EmcValue.of(32), result.values().get(wetDust));
        assertEquals(Map.of(bucket, 1), result.derivations().get(wetDust).returnedRemainders());
    }

    @Test
    void explicitValuesAreNeverOverwritten() {
        EmcRecipe cheapDiamond = EmcRecipe.of(key("example:cheap_diamond"),
            List.of(EmcIngredient.of(STICK, 1)), key("minecraft:diamond"), 1);

        EmcMappingResult result = RecipeEmcMapper.map(
            Map.of(STICK, EmcValue.of(4), key("minecraft:diamond"), EmcValue.of(8192)),
            List.of(cheapDiamond));

        assertEquals(EmcValue.of(8192), result.values().get(key("minecraft:diamond")));
        assertFalse(result.derivations().containsKey(key("minecraft:diamond")));
    }

    @Test
    void directAndIndirectFeedbackCannotLowerDerivedValues() {
        EmcKey seed = key("example:seed");
        EmcKey first = key("example:first");
        EmcKey second = key("example:second");
        List<EmcRecipe> recipes = List.of(
            EmcRecipe.of(key("example:seed_to_first"), List.of(EmcIngredient.of(seed, 1)), first, 1),
            EmcRecipe.of(key("example:first_to_second"), List.of(EmcIngredient.of(first, 1)), second, 2),
            EmcRecipe.of(key("example:second_to_first"), List.of(EmcIngredient.of(second, 1)), first, 2),
            EmcRecipe.of(key("example:self_duplicate"), List.of(EmcIngredient.of(first, 1)), first, 2)
        );

        EmcMappingResult result = RecipeEmcMapper.map(Map.of(seed, EmcValue.of(100)), recipes);

        assertEquals(EmcValue.of(100), result.values().get(first));
        assertEquals(EmcValue.of(50), result.values().get(second));
    }

    @Test
    void unresolvedAndExcludedRecipesAreReportedCorrectly() {
        EmcKey unknown = key("example:unknown");
        EmcKey output = key("example:output");
        EmcRecipe unresolved = EmcRecipe.of(key("example:unresolved"),
            List.of(EmcIngredient.of(unknown, 1)), output, 1);
        EmcRecipe excluded = new EmcRecipe(key("example:excluded"),
            List.of(EmcIngredient.of(unknown, 1)), key("example:ignored"), 1, List.of(), true);

        EmcMappingResult result = RecipeEmcMapper.map(Map.of(), List.of(excluded, unresolved));

        assertEquals(java.util.Set.of(unresolved.id()), result.unresolvedRecipes());
        assertTrue(result.values().isEmpty());
    }

    @Test
    void resultDoesNotDependOnRecipeInputOrder() {
        EmcRecipe planks = EmcRecipe.of(key("minecraft:planks"),
            List.of(EmcIngredient.of(LOG, 1)), PLANKS, 4);
        EmcRecipe sticks = EmcRecipe.of(key("minecraft:sticks"),
            List.of(EmcIngredient.of(PLANKS, 2)), STICK, 4);

        EmcMappingResult forward = RecipeEmcMapper.map(Map.of(LOG, EmcValue.of(32)), List.of(planks, sticks));
        EmcMappingResult reverse = RecipeEmcMapper.map(Map.of(LOG, EmcValue.of(32)), List.of(sticks, planks));

        assertEquals(forward, reverse);
        assertEquals(EmcValue.of(4), forward.values().get(STICK));
    }

    private static EmcKey key(String value) {
        return EmcKey.parse(value);
    }
}
