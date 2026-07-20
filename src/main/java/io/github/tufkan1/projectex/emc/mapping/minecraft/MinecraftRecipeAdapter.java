package io.github.tufkan1.projectex.emc.mapping.minecraft;

import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.content.KleinStarTier;
import io.github.tufkan1.projectex.content.recipe.KleinStarUpgradeRecipe;
import io.github.tufkan1.projectex.emc.mapping.EmcIngredient;
import io.github.tufkan1.projectex.emc.mapping.EmcRecipe;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

/** Converts safe, static vanilla/Fabric recipes into loader-neutral EMC equations. */
public final class MinecraftRecipeAdapter {
    private MinecraftRecipeAdapter() {
    }

    public static AdaptationResult adapt(Collection<RecipeHolder<?>> holders) {
        List<EmcRecipe> recipes = new ArrayList<>();
        Map<EmcKey, String> exclusions = new LinkedHashMap<>();
        holders.stream()
            .sorted(java.util.Comparator.comparing(holder -> holder.id().identifier()))
            .forEach(holder -> {
                if (holder.value() instanceof KleinStarUpgradeRecipe) {
                    recipes.addAll(kleinStarUpgrades(holder));
                    return;
                }
                adaptOne(holder).ifPresentOrElse(
                    recipes::add,
                    () -> exclusions.put(key(holder.id().identifier()), exclusionReason(holder.value()))
                );
            });
        return new AdaptationResult(recipes, exclusions);
    }

    /** The dynamic recipe preserves stored EMC, but its empty-container cost is static. */
    private static List<EmcRecipe> kleinStarUpgrades(RecipeHolder<?> holder) {
        ArrayList<EmcRecipe> recipes = new ArrayList<>();
        for (KleinStarTier source : KleinStarTier.values()) {
            KleinStarTier target = source.next();
            if (target == null) continue;
            recipes.add(new EmcRecipe(
                new EmcKey(holder.id().identifier().getNamespace(),
                    holder.id().identifier().getPath() + "/" + target.serializedName()),
                List.of(
                    EmcIngredient.of(new EmcKey("projectex", source.serializedName()), 4),
                    EmcIngredient.of(new EmcKey("projectex", "aeternalis_fuel"), 1)
                ),
                new EmcKey("projectex", target.serializedName()), 1, List.of(), false
            ));
        }
        return List.copyOf(recipes);
    }

    static Optional<EmcRecipe> adaptOne(RecipeHolder<?> holder) {
        Recipe<?> recipe = holder.value();
        if (recipe.isSpecial()) {
            return Optional.empty();
        }
        PlacementInfo placement = recipe.placementInfo();
        if (placement.isImpossibleToPlace() || placement.ingredients().isEmpty()) {
            return Optional.empty();
        }

        Optional<ItemStackTemplate> result = simpleResult(recipe.display());
        if (result.isEmpty() || !result.orElseThrow().components().isEmpty()) {
            return Optional.empty();
        }
        ItemStackTemplate output = result.orElseThrow();
        Optional<EmcKey> outputKey = itemKey(output.item());
        if (outputKey.isEmpty() || output.count() <= 0 || outputKey.orElseThrow().toString().equals("minecraft:air")) {
            return Optional.empty();
        }

        int[] counts = new int[placement.ingredients().size()];
        placement.slotsToIngredientIndex().forEach(index -> {
            if (index >= 0 && index < counts.length) {
                counts[index]++;
            }
        });
        List<EmcIngredient> inputs = new ArrayList<>();
        List<EmcIngredient> remainders = new ArrayList<>();
        for (int index = 0; index < placement.ingredients().size(); index++) {
            int count = counts[index] == 0 ? 1 : counts[index];
            Ingredient ingredient = placement.ingredients().get(index);
            List<Holder<Item>> ingredientItems = ingredientItems(ingredient.display());
            List<EmcKey> alternatives = ingredientItems.stream()
                .map(MinecraftRecipeAdapter::itemKey)
                .flatMap(Optional::stream)
                .filter(item -> !item.toString().equals("minecraft:air"))
                .distinct()
                .sorted()
                .toList();
            if (alternatives.isEmpty()) {
                return Optional.empty();
            }
            inputs.add(new EmcIngredient(alternatives, count));

            Optional<Remainder> remainder = commonRemainder(ingredientItems);
            if (remainder.isEmpty()) {
                return Optional.empty();
            }
            Remainder value = remainder.orElseThrow();
            if (value.item() != null) {
                remainders.add(EmcIngredient.of(value.item(), Math.multiplyExact(count, value.count())));
            }
        }
        return Optional.of(new EmcRecipe(
            key(holder.id().identifier()),
            inputs,
            outputKey.orElseThrow(),
            output.count(),
            remainders,
            false
        ));
    }

    private static Optional<ItemStackTemplate> simpleResult(List<RecipeDisplay> displays) {
        List<ItemStackTemplate> results = displays.stream()
            .map(RecipeDisplay::result)
            .map(MinecraftRecipeAdapter::simpleStack)
            .flatMap(Optional::stream)
            .distinct()
            .toList();
        return results.size() == 1 ? Optional.of(results.getFirst()) : Optional.empty();
    }

    private static Optional<ItemStackTemplate> simpleStack(SlotDisplay display) {
        if (display instanceof SlotDisplay.ItemStackSlotDisplay stackDisplay) {
            return Optional.of(stackDisplay.stack());
        }
        if (display instanceof SlotDisplay.ItemSlotDisplay itemDisplay) {
            return Optional.of(new ItemStackTemplate(itemDisplay.item().value()));
        }
        return Optional.empty();
    }

    private static List<Holder<Item>> ingredientItems(SlotDisplay display) {
        if (display instanceof SlotDisplay.ItemSlotDisplay itemDisplay) {
            return List.of(itemDisplay.item());
        }
        if (display instanceof SlotDisplay.ItemStackSlotDisplay stackDisplay
            && stackDisplay.stack().components().isEmpty()) {
            return List.of(stackDisplay.stack().item());
        }
        if (display instanceof SlotDisplay.WithRemainder withRemainder) {
            return ingredientItems(withRemainder.input());
        }
        if (display instanceof SlotDisplay.TagSlotDisplay tagDisplay) {
            List<Holder<Item>> items = new ArrayList<>();
            BuiltInRegistries.ITEM.getTagOrEmpty(tagDisplay.tag()).forEach(items::add);
            return List.copyOf(items);
        }
        if (display instanceof SlotDisplay.Composite composite) {
            return composite.contents().stream()
                .map(MinecraftRecipeAdapter::ingredientItems)
                .flatMap(Collection::stream)
                .distinct()
                .toList();
        }
        return List.of();
    }

    /** Empty means alternatives disagree or use component-bearing remainders. */
    private static Optional<Remainder> commonRemainder(List<Holder<Item>> alternatives) {
        Remainder expected = null;
        for (Holder<Item> alternative : alternatives) {
            ItemStackTemplate template = alternative.value().getCraftingRemainder();
            Remainder current;
            if (template == null || template.count() <= 0) {
                current = Remainder.NONE;
            } else if (!template.components().isEmpty()) {
                return Optional.empty();
            } else {
                Optional<EmcKey> remainderKey = itemKey(template.item());
                if (remainderKey.isEmpty()) {
                    return Optional.empty();
                }
                current = new Remainder(remainderKey.orElseThrow(), template.count());
            }
            if (expected != null && !expected.equals(current)) {
                return Optional.empty();
            }
            expected = current;
        }
        return Optional.ofNullable(expected);
    }

    private static Optional<EmcKey> itemKey(Holder<Item> holder) {
        return holder.unwrapKey().map(resourceKey -> key(resourceKey.identifier()));
    }

    private static EmcKey key(Identifier identifier) {
        return new EmcKey(identifier.getNamespace(), identifier.getPath());
    }

    private static String exclusionReason(Recipe<?> recipe) {
        if (recipe.isSpecial()) {
            return "special-or-dynamic";
        }
        if (recipe.placementInfo().isImpossibleToPlace() || recipe.placementInfo().ingredients().isEmpty()) {
            return "no-static-inputs";
        }
        return "unsupported-output-components-or-remainder";
    }

    private record Remainder(EmcKey item, int count) {
        private static final Remainder NONE = new Remainder(null, 0);
    }

    public record AdaptationResult(List<EmcRecipe> recipes, Map<EmcKey, String> exclusions) {
        public AdaptationResult {
            recipes = List.copyOf(recipes);
            exclusions = java.util.Collections.unmodifiableMap(new java.util.TreeMap<>(exclusions));
        }
    }
}
