package io.github.tufkan1.projectex.teleport;

import java.util.Locale;

/** Validated server edit policy for player-bound Alchemical Books. */
public final class AlchemicalBookConfig {
    public static final String EDIT_POLICY = "projectex.alchemicalBook.editPolicy";
    private static volatile EditPolicy policy = load();
    private AlchemicalBookConfig() { }
    public static EditPolicy policy() { return policy; }
    public static void reload() { policy = load(); }
    static EditPolicy load() {
        return switch (System.getProperty(EDIT_POLICY, "owner_only").trim().toLowerCase(Locale.ROOT)) {
            case "owner_only" -> EditPolicy.OWNER_ONLY;
            case "operator_only" -> EditPolicy.OPERATOR_ONLY;
            case "enabled" -> EditPolicy.ENABLED;
            default -> throw new IllegalArgumentException(EDIT_POLICY + " must be owner_only, operator_only, or enabled");
        };
    }
    public enum EditPolicy { OWNER_ONLY, OPERATOR_ONLY, ENABLED }
}
