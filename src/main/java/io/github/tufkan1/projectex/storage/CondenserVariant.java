package io.github.tufkan1.projectex.storage;

import java.util.Objects;

/** Component-aware persistent item identity used by condenser targets and outputs. */
public record CondenserVariant(String itemId, String componentsJson) implements Comparable<CondenserVariant> {
    public CondenserVariant {
        Objects.requireNonNull(itemId, "itemId");
        if (itemId.isBlank()) {
            throw new IllegalArgumentException("Condenser item id cannot be blank");
        }
    }

    public static CondenserVariant item(String itemId) {
        return new CondenserVariant(itemId, null);
    }

    @Override
    public int compareTo(CondenserVariant other) {
        int itemOrder = itemId.compareTo(other.itemId);
        if (itemOrder != 0) {
            return itemOrder;
        }
        if (componentsJson == null) {
            return other.componentsJson == null ? 0 : -1;
        }
        return other.componentsJson == null ? 1 : componentsJson.compareTo(other.componentsJson);
    }
}
