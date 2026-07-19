package io.github.tufkan1.projectex.emc.data;

import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcMatch;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.util.Objects;

/** One explicit value, alias, or removal from a versioned EMC data file. */
public record EmcDefinition(
    EmcKey item,
    String componentsJson,
    Kind kind,
    EmcValue value,
    EmcKey alias
) {
    public EmcDefinition {
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(kind, "kind");
        switch (kind) {
            case VALUE -> {
                Objects.requireNonNull(value, "value");
                if (alias != null) {
                    throw new IllegalArgumentException("A value definition cannot have an alias");
                }
            }
            case ALIAS -> {
                Objects.requireNonNull(alias, "alias");
                if (value != null) {
                    throw new IllegalArgumentException("An alias definition cannot have a value");
                }
            }
            case REMOVE -> {
                if (value != null || alias != null) {
                    throw new IllegalArgumentException("A removal cannot have a value or alias");
                }
            }
        }
    }

    /** Stable identity used to detect duplicates independently of JSON object key order. */
    public String matchKey() {
        return match().toString();
    }

    public EmcMatch match() {
        return new EmcMatch(item, componentsJson);
    }

    public enum Kind {
        VALUE,
        ALIAS,
        REMOVE
    }
}
