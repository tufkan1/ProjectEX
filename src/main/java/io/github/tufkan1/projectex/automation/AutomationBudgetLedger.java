package io.github.tufkan1.projectex.automation;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Tick-local request/value budget with replay rejection. */
final class AutomationBudgetLedger {
    private long tick = Long.MIN_VALUE;
    private int requests;
    private EmcValue transferred = EmcValue.ZERO;
    private final Set<UUID> requestIds = new HashSet<>();

    synchronized AutomationAuditEvent.Failure reserve(
        EmcLinkRequest request,
        EmcAutomationTier tier
    ) {
        return reserve(request.requestId(), request.tick(), request.amount(), tier);
    }

    synchronized AutomationAuditEvent.Failure reserve(
        UUID requestId,
        long requestedTick,
        EmcValue amount,
        EmcAutomationTier tier
    ) {
        if (tick != Long.MIN_VALUE && requestedTick < tick) {
            return AutomationAuditEvent.Failure.STALE_TICK;
        }
        if (requestedTick != tick) {
            tick = requestedTick;
            requests = 0;
            transferred = EmcValue.ZERO;
            requestIds.clear();
        }
        if (!requestIds.add(requestId)) {
            return AutomationAuditEvent.Failure.REPLAYED;
        }
        if (amount.compareTo(tier.maximumPerRequest()) > 0) {
            return AutomationAuditEvent.Failure.REQUEST_LIMIT;
        }
        if (requests >= tier.maximumRequestsPerTick()
            || transferred.add(amount).compareTo(tier.maximumPerTick()) > 0) {
            return AutomationAuditEvent.Failure.TICK_BUDGET;
        }
        requests++;
        transferred = transferred.add(amount);
        return AutomationAuditEvent.Failure.NONE;
    }
}
