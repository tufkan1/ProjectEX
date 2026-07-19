package io.github.tufkan1.projectex.content;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

/** Validated server policy for the high-impact Knowledge Tome unlock. */
public final class KnowledgeTomePolicy {
    public static final String PROPERTY = "projectex.knowledgeTome.policy";
    private static volatile Mode mode = parse(System.getProperty(PROPERTY, "consume"));

    private KnowledgeTomePolicy() { }

    public static Mode mode() { return mode; }
    public static void reload() { mode = parse(System.getProperty(PROPERTY, "consume")); }

    public static Mode parse(String value) {
        return switch (value.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "disabled" -> Mode.DISABLED;
            case "consume" -> Mode.CONSUME;
            case "operator_only" -> Mode.OPERATOR_ONLY;
            default -> throw new IllegalArgumentException(PROPERTY
                + " must be disabled, consume, or operator_only");
        };
    }

    public enum Mode {
        DISABLED,
        CONSUME,
        OPERATOR_ONLY;

        public boolean permits(ServerPlayer player) {
            return this == CONSUME || this == OPERATOR_ONLY
                && player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        }
    }
}
