package io.github.tufkan1.projectex.emc.mapping;

import io.github.tufkan1.projectex.api.emc.EmcKey;
import java.util.List;
import java.util.Objects;

/** Normalized one-output recipe equation consumed by the EMC mapper. */
public record EmcRecipe(
    EmcKey id,
    List<EmcIngredient> inputs,
    EmcKey output,
    int outputCount,
    List<EmcIngredient> remainders,
    boolean excluded
) {
    public EmcRecipe {
        Objects.requireNonNull(id, "id");
        inputs = List.copyOf(inputs);
        Objects.requireNonNull(output, "output");
        remainders = List.copyOf(remainders);
        if (inputs.isEmpty()) {
            throw new IllegalArgumentException("An EMC recipe needs at least one input");
        }
        if (outputCount <= 0) {
            throw new IllegalArgumentException("Recipe output count must be positive");
        }
    }

    public static EmcRecipe of(EmcKey id, List<EmcIngredient> inputs, EmcKey output, int outputCount) {
        return new EmcRecipe(id, inputs, output, outputCount, List.of(), false);
    }
}
