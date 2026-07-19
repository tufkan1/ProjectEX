package io.github.tufkan1.projectex.machine;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.util.Objects;

/** Data-ready collector fuel transformation with a non-negative exact cost. */
public record FuelUpgradeRule(
    String inputId,
    String outputId,
    EmcValue inputValue,
    EmcValue outputValue
) {
    public FuelUpgradeRule {
        Objects.requireNonNull(inputId, "inputId");
        Objects.requireNonNull(outputId, "outputId");
        Objects.requireNonNull(inputValue, "inputValue");
        Objects.requireNonNull(outputValue, "outputValue");
        if (inputId.isBlank() || outputId.isBlank() || outputValue.compareTo(inputValue) < 0) {
            throw new IllegalArgumentException("Fuel upgrade must have ids and cannot reduce EMC value");
        }
    }

    public EmcValue cost() {
        return outputValue.subtract(inputValue);
    }

    public Upgrade apply(String itemId, MachineBuffer buffer) {
        if (!inputId.equals(itemId) || buffer.stored().compareTo(cost()) < 0) {
            return new Upgrade(itemId, false, EmcValue.ZERO);
        }
        EmcValue spent = buffer.extract(cost());
        return new Upgrade(outputId, true, spent);
    }

    public record Upgrade(String resultId, boolean upgraded, EmcValue spent) {
    }
}
