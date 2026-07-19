package io.github.tufkan1.projectex.content;

/** Fail-closed server gate for destructive catalyst powers. */
public final class DestructiveCatalystPolicy {
    public static final String ENABLED_PROPERTY = "projectex.destructiveCatalysts.enabled";
    private static volatile boolean enabled = parse(
        System.getProperty(ENABLED_PROPERTY, "true"));

    private DestructiveCatalystPolicy() { }
    public static boolean enabled() { return enabled; }
    public static void reload() { enabled = parse(System.getProperty(ENABLED_PROPERTY, "true")); }

    public static boolean parse(String value) {
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        throw new IllegalArgumentException(ENABLED_PROPERTY + " must be true or false");
    }
}
