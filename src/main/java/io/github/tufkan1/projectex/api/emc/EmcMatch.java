package io.github.tufkan1.projectex.api.emc;

import java.util.Objects;

/** Item identifier plus optional canonical component constraints. */
public record EmcMatch(EmcKey item, String componentsJson) implements Comparable<EmcMatch> {
    public EmcMatch {
        Objects.requireNonNull(item, "item");
    }

    public static EmcMatch item(EmcKey item) {
        return new EmcMatch(item, null);
    }

    @Override
    public int compareTo(EmcMatch other) {
        int itemOrder = item.compareTo(other.item);
        if (itemOrder != 0) {
            return itemOrder;
        }
        if (componentsJson == null) {
            return other.componentsJson == null ? 0 : -1;
        }
        return other.componentsJson == null ? 1 : componentsJson.compareTo(other.componentsJson);
    }

    @Override
    public String toString() {
        return componentsJson == null ? item.toString() : item + "|" + componentsJson;
    }
}
