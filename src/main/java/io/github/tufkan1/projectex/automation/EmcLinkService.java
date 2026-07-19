package io.github.tufkan1.projectex.automation;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.player.PlayerAlchemyState;
import java.util.Objects;

/** Permission-aware, replay-safe CAS transaction service for one persisted EMC Link. */
public final class EmcLinkService {
    private static final int MAX_CAS_ATTEMPTS = 8;
    private final EmcAutomationTier tier;
    private final AutomationAccess access;
    private final EmcLinkFilter insertFilter;
    private final EmcLinkFilter extractFilter;
    private final AutomationAccount account;
    private final AutomationAuditSink audit;
    private final AutomationBudgetLedger budget = new AutomationBudgetLedger();

    public EmcLinkService(
        EmcAutomationTier tier,
        AutomationAccess access,
        EmcLinkFilter insertFilter,
        EmcLinkFilter extractFilter,
        AutomationAccount account,
        AutomationAuditSink audit
    ) {
        this.tier = Objects.requireNonNull(tier, "tier");
        this.access = Objects.requireNonNull(access, "access");
        this.insertFilter = Objects.requireNonNull(insertFilter, "insertFilter");
        this.extractFilter = Objects.requireNonNull(extractFilter, "extractFilter");
        this.account = Objects.requireNonNull(account, "account");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    public Transfer transfer(EmcLinkRequest request, AutomationAuthority authority) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(authority, "authority");
        PlayerAlchemyState observed = account.snapshot();
        if (!access.permits(authority, request.operation())) {
            return reject(request, authority, observed, AutomationAuditEvent.Failure.UNAUTHORIZED);
        }
        EmcLinkFilter filter = request.operation() == AutomationOperation.INSERT_EMC
            ? insertFilter : extractFilter;
        if (!filter.permits(request.item(), tier.maximumFilterEntries())) {
            return reject(request, authority, observed, AutomationAuditEvent.Failure.FILTERED);
        }
        AutomationAuditEvent.Failure budgetFailure = budget.reserve(request, tier);
        if (budgetFailure != AutomationAuditEvent.Failure.NONE) {
            return reject(request, authority, observed, budgetFailure);
        }

        for (int attempt = 0; attempt < MAX_CAS_ATTEMPTS; attempt++) {
            PlayerAlchemyState before = account.snapshot();
            PlayerAlchemyState after;
            if (request.operation() == AutomationOperation.EXTRACT_EMC) {
                var debited = before.debit(request.amount());
                if (debited.isEmpty()) {
                    return reject(request, authority, before,
                        AutomationAuditEvent.Failure.INSUFFICIENT_EMC);
                }
                after = debited.orElseThrow();
            } else {
                try {
                    after = before.credit(request.amount());
                } catch (IllegalArgumentException exception) {
                    return reject(request, authority, before,
                        AutomationAuditEvent.Failure.BALANCE_LIMIT);
                }
            }
            if (account.compareAndSet(before, after)) {
                AutomationAuditEvent event = event(
                    request, authority, request.amount(), before.balance(), after.balance(),
                    AutomationAuditEvent.Failure.NONE
                );
                audit.record(event);
                return new Transfer(request.amount(), request.amount(), event.failure());
            }
        }
        return reject(request, authority, account.snapshot(), AutomationAuditEvent.Failure.CONTENTION);
    }

    private Transfer reject(
        EmcLinkRequest request,
        AutomationAuthority authority,
        PlayerAlchemyState state,
        AutomationAuditEvent.Failure failure
    ) {
        AutomationAuditEvent event = event(
            request, authority, EmcValue.ZERO, state.balance(), state.balance(), failure
        );
        audit.record(event);
        return new Transfer(request.amount(), EmcValue.ZERO, failure);
    }

    private static AutomationAuditEvent event(
        EmcLinkRequest request,
        AutomationAuthority authority,
        EmcValue transferred,
        EmcValue before,
        EmcValue after,
        AutomationAuditEvent.Failure failure
    ) {
        return new AutomationAuditEvent(
            request.requestId(), request.tick(), request.operation(), authority.actor(), request.item(),
            request.amount(), transferred, before, after, failure
        );
    }

    public record Transfer(
        EmcValue requested,
        EmcValue transferred,
        AutomationAuditEvent.Failure failure
    ) {
        public Transfer {
            Objects.requireNonNull(requested, "requested");
            Objects.requireNonNull(transferred, "transferred");
            Objects.requireNonNull(failure, "failure");
        }

        public boolean successful() {
            return failure == AutomationAuditEvent.Failure.NONE;
        }
    }
}
