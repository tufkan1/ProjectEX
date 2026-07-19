package io.github.tufkan1.projectex.emc.mapping;

import io.github.tufkan1.projectex.api.emc.EmcKey;
import java.util.List;

/** One counted recipe input whose alternatives model an item or tag choice. */
public record EmcIngredient(List<EmcKey> alternatives, int count) {
    public EmcIngredient {
        alternatives = alternatives.stream().distinct().sorted().toList();
        if (alternatives.isEmpty()) {
            throw new IllegalArgumentException("An EMC ingredient needs at least one alternative");
        }
        if (count <= 0) {
            throw new IllegalArgumentException("Ingredient count must be positive");
        }
    }

    public static EmcIngredient of(EmcKey item, int count) {
        return new EmcIngredient(List.of(item), count);
    }
}
