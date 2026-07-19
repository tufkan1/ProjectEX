package io.github.tufkan1.projectex.content.component;

import java.util.Locale;

/** Supported deterministic target shapes for chargeable active items. */
public enum ActiveItemMode {
    CUBE,
    PANEL,
    LINE;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public ActiveItemMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static ActiveItemMode parse(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
