package io.github.tufkan1.projectex.emc.mapping;

import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Deterministic minimum-cost fixed-point mapper with dependency-cycle protection. */
public final class RecipeEmcMapper {
    private RecipeEmcMapper() {
    }

    public static EmcMappingResult map(Map<EmcKey, EmcValue> explicitValues, List<EmcRecipe> inputRecipes) {
        Map<EmcKey, EmcValue> values = new TreeMap<>(explicitValues);
        Set<EmcKey> locked = Set.copyOf(explicitValues.keySet());
        Map<EmcKey, EmcDerivation> derivations = new TreeMap<>();
        Map<EmcKey, Set<EmcKey>> dependencies = new HashMap<>();
        List<EmcRecipe> recipes = inputRecipes.stream()
            .filter(recipe -> !recipe.excluded())
            .sorted(java.util.Comparator.comparing(EmcRecipe::id))
            .toList();

        int uniqueOutputs = (int) recipes.stream().map(EmcRecipe::output).distinct().count();
        int maxPasses = Math.max(1, recipes.size() * Math.max(1, uniqueOutputs) + 1);
        int passes = 0;
        boolean changed;
        do {
            changed = false;
            passes++;
            for (EmcRecipe recipe : recipes) {
                if (locked.contains(recipe.output())) {
                    continue;
                }
                Optional<Calculation> calculation = calculate(recipe, values, dependencies);
                if (calculation.isEmpty()) {
                    continue;
                }
                Calculation candidate = calculation.orElseThrow();
                EmcValue current = values.get(recipe.output());
                if (current == null || candidate.value().compareTo(current) < 0) {
                    values.put(recipe.output(), candidate.value());
                    derivations.put(recipe.output(), new EmcDerivation(
                        candidate.value(), recipe.id(), candidate.inputs(), candidate.remainders()));
                    dependencies.put(recipe.output(), candidate.dependencies());
                    changed = true;
                }
            }
        } while (changed && passes < maxPasses);

        Set<EmcKey> unresolved = new TreeSet<>();
        for (EmcRecipe recipe : recipes) {
            if (!values.containsKey(recipe.output())) {
                unresolved.add(recipe.id());
            }
        }
        return new EmcMappingResult(values, derivations, unresolved, passes);
    }

    private static Optional<Calculation> calculate(
        EmcRecipe recipe,
        Map<EmcKey, EmcValue> values,
        Map<EmcKey, Set<EmcKey>> knownDependencies
    ) {
        Map<EmcKey, Integer> chosenInputs = new LinkedHashMap<>();
        Map<EmcKey, Integer> chosenRemainders = new LinkedHashMap<>();
        Set<EmcKey> dependencies = new HashSet<>();
        BigInteger inputCost = BigInteger.ZERO;
        for (EmcIngredient ingredient : recipe.inputs()) {
            Optional<Choice> choice = choose(ingredient, values);
            if (choice.isEmpty()) {
                return Optional.empty();
            }
            Choice selected = choice.orElseThrow();
            if (selected.item().equals(recipe.output())
                || knownDependencies.getOrDefault(selected.item(), Set.of()).contains(recipe.output())) {
                return Optional.empty();
            }
            inputCost = inputCost.add(selected.total());
            chosenInputs.merge(selected.item(), ingredient.count(), Integer::sum);
            dependencies.add(selected.item());
            dependencies.addAll(knownDependencies.getOrDefault(selected.item(), Set.of()));
        }

        BigInteger returnedCost = BigInteger.ZERO;
        for (EmcIngredient remainder : recipe.remainders()) {
            Optional<Choice> choice = choose(remainder, values);
            if (choice.isEmpty()) {
                continue;
            }
            Choice selected = choice.orElseThrow();
            if (selected.item().equals(recipe.output())) {
                return Optional.empty();
            }
            returnedCost = returnedCost.add(selected.total());
            chosenRemainders.merge(selected.item(), remainder.count(), Integer::sum);
        }

        BigInteger netCost = inputCost.subtract(returnedCost);
        if (netCost.signum() <= 0) {
            return Optional.empty();
        }
        BigInteger divisor = BigInteger.valueOf(recipe.outputCount());
        BigInteger value = netCost.add(divisor).subtract(BigInteger.ONE).divide(divisor);
        return Optional.of(new Calculation(
            new EmcValue(value),
            Map.copyOf(chosenInputs),
            Map.copyOf(chosenRemainders),
            Set.copyOf(dependencies)
        ));
    }

    private static Optional<Choice> choose(EmcIngredient ingredient, Map<EmcKey, EmcValue> values) {
        return ingredient.alternatives().stream()
            .filter(values::containsKey)
            .map(item -> new Choice(
                item,
                values.get(item).amount().multiply(BigInteger.valueOf(ingredient.count()))
            ))
            .min(java.util.Comparator.comparing(Choice::total).thenComparing(Choice::item));
    }

    private record Choice(EmcKey item, BigInteger total) {
    }

    private record Calculation(
        EmcValue value,
        Map<EmcKey, Integer> inputs,
        Map<EmcKey, Integer> remainders,
        Set<EmcKey> dependencies
    ) {
    }
}
