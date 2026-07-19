package io.github.tufkan1.projectex.matter;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.util.UUID;

/** Privacy-minimal server audit record for a committed bounded matter action. */
public record MatterActionAuditEvent(
    UUID actor,
    String tier,
    String action,
    int attempted,
    int committed,
    int protectionDenied,
    EmcValue emcSpent,
    long gameTick
) {
    public MatterActionAuditEvent {
        if (attempted < 0 || committed < 0 || protectionDenied < 0 || committed > attempted
            || protectionDenied > attempted || gameTick < 0) {
            throw new IllegalArgumentException("Invalid matter audit event");
        }
        java.util.Objects.requireNonNull(actor, "actor");
        java.util.Objects.requireNonNull(tier, "tier");
        java.util.Objects.requireNonNull(action, "action");
        java.util.Objects.requireNonNull(emcSpent, "emcSpent");
    }
}
