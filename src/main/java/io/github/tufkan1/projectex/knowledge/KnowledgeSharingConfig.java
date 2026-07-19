package io.github.tufkan1.projectex.knowledge;

import java.time.Duration;
import java.util.Locale;

/** Validated server-side policy for signed knowledge sharing. */
public final class KnowledgeSharingConfig {
    public static final String POLICY = "projectex.knowledgeSharing.policy";
    public static final String LIFETIME_HOURS = "projectex.knowledgeSharing.lifetimeHours";
    private static volatile Snapshot snapshot = load();

    private KnowledgeSharingConfig() { }

    public static Snapshot snapshot() { return snapshot; }
    public static void reload() { snapshot = load(); }

    static Snapshot load() {
        KnowledgeShareWorkflow.SharingPolicy policy = switch (
            System.getProperty(POLICY, "enabled").trim().toLowerCase(Locale.ROOT)
        ) {
            case "enabled" -> KnowledgeShareWorkflow.SharingPolicy.ENABLED;
            case "creative_only" -> KnowledgeShareWorkflow.SharingPolicy.CREATIVE_ONLY;
            case "disabled" -> KnowledgeShareWorkflow.SharingPolicy.DISABLED;
            default -> throw new IllegalArgumentException(POLICY + " must be enabled, creative_only, or disabled");
        };
        int hours;
        try {
            hours = Integer.parseInt(System.getProperty(LIFETIME_HOURS, "24"));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(LIFETIME_HOURS + " must be an integer", exception);
        }
        if (hours < 1 || hours > KnowledgeSnapshotSigner.MAX_LIFETIME.toHours()) {
            throw new IllegalArgumentException(LIFETIME_HOURS + " is outside safe bounds");
        }
        return new Snapshot(policy, Duration.ofHours(hours));
    }

    public record Snapshot(KnowledgeShareWorkflow.SharingPolicy policy, Duration lifetime) { }
}
