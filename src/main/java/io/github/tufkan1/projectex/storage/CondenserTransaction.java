package io.github.tufkan1.projectex.storage;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Atomic evaluator that conserves exact EMC while consuming inputs and producing a target. */
public final class CondenserTransaction {
    private CondenserTransaction() {
    }

    public static Result evaluate(
        CondenserVariant target,
        EmcValue targetValue,
        EmcValue stored,
        List<Input> inputs,
        int outputSpace,
        int inputBudget
    ) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(targetValue, "targetValue");
        Objects.requireNonNull(stored, "stored");
        List<Input> safeInputs = List.copyOf(inputs);
        if (targetValue.equals(EmcValue.ZERO) || outputSpace < 0 || inputBudget < 0) {
            throw new IllegalArgumentException("Condenser target and budgets must be positive");
        }
        if (outputSpace == 0) {
            return unchanged(stored, safeInputs.size());
        }

        EmcValue available = stored;
        int remainingBudget = inputBudget;
        List<Integer> consumed = new ArrayList<>(safeInputs.size());
        for (Input input : safeInputs) {
            int count = 0;
            if (remainingBudget > 0 && !input.variant.equals(target)
                && !input.value.equals(EmcValue.ZERO)) {
                count = Math.min(input.count, remainingBudget);
                available = available.add(input.value.multiply(count));
                remainingBudget -= count;
            }
            consumed.add(count);
        }

        BigInteger possible = available.amount().divide(targetValue.amount());
        int produced = possible.min(BigInteger.valueOf(outputSpace)).intValueExact();
        if (produced == 0 && stored.equals(EmcValue.ZERO)) {
            // Avoid consuming inventory merely to strand sub-target EMC when no prior buffer exists.
            return unchanged(stored, safeInputs.size());
        }
        EmcValue spent = targetValue.multiply(produced);
        EmcValue resultingStored = available.subtract(spent);
        Result result = new Result(resultingStored, List.copyOf(consumed), produced);
        verifyConservation(stored, safeInputs, targetValue, result);
        return result;
    }

    private static Result unchanged(EmcValue stored, int inputs) {
        return new Result(stored, java.util.Collections.nCopies(inputs, 0), 0);
    }

    private static void verifyConservation(
        EmcValue initialStored,
        List<Input> inputs,
        EmcValue targetValue,
        Result result
    ) {
        EmcValue before = initialStored;
        for (int index = 0; index < inputs.size(); index++) {
            before = before.add(inputs.get(index).value.multiply(result.consumedCounts.get(index)));
        }
        EmcValue after = result.stored.add(targetValue.multiply(result.produced));
        if (!before.equals(after)) {
            throw new IllegalStateException("Condenser transaction did not conserve EMC");
        }
    }

    public record Input(CondenserVariant variant, EmcValue value, int count) {
        public Input {
            Objects.requireNonNull(variant, "variant");
            Objects.requireNonNull(value, "value");
            if (count < 0) {
                throw new IllegalArgumentException("Input count cannot be negative");
            }
        }
    }

    public record Result(EmcValue stored, List<Integer> consumedCounts, int produced) {
        public Result {
            Objects.requireNonNull(stored, "stored");
            consumedCounts = List.copyOf(consumedCounts);
            if (consumedCounts.stream().anyMatch(count -> count < 0) || produced < 0) {
                throw new IllegalArgumentException("Condenser result counts cannot be negative");
            }
        }

        public boolean changed() {
            return produced > 0 || consumedCounts.stream().anyMatch(count -> count > 0);
        }
    }
}
