package io.github.tufkan1.projectex.automation;

import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcSnapshot;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.player.PlayerAlchemyState;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Supplier;

/** Permission-aware knowledge and atomic crafting service for one persisted interface. */
public final class TransmutationInterfaceService {
    private static final int MAX_COMMIT_ATTEMPTS = 8;
    private final EmcAutomationTier tier;
    private final AutomationAccess access;
    private final CraftingTransactionTarget target;
    private final Supplier<EmcSnapshot> emc;
    private final AutomationAuditSink audit;
    private final AutomationBudgetLedger budget = new AutomationBudgetLedger();

    public TransmutationInterfaceService(
        EmcAutomationTier tier,
        AutomationAccess access,
        CraftingTransactionTarget target,
        Supplier<EmcSnapshot> emc,
        AutomationAuditSink audit
    ) {
        this.tier = Objects.requireNonNull(tier, "tier");
        this.access = Objects.requireNonNull(access, "access");
        this.target = Objects.requireNonNull(target, "target");
        this.emc = Objects.requireNonNull(emc, "emc");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    public KnowledgeResult available(KnowledgeQuery query, AutomationAuthority authority) {
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(authority, "authority");
        PlayerAlchemyState observed = target.snapshot().account();
        if (!access.permits(authority, AutomationOperation.ENUMERATE_KNOWLEDGE)) {
            audit(query, authority, observed, AutomationAuditEvent.Failure.UNAUTHORIZED);
            return new KnowledgeResult(Collections.unmodifiableSortedSet(new TreeSet<>()),
                AutomationAuditEvent.Failure.UNAUTHORIZED);
        }
        if (query.candidates().size() > tier.maximumFilterEntries()) {
            audit(query, authority, observed, AutomationAuditEvent.Failure.REQUEST_LIMIT);
            return new KnowledgeResult(Collections.unmodifiableSortedSet(new TreeSet<>()),
                AutomationAuditEvent.Failure.REQUEST_LIMIT);
        }
        AutomationAuditEvent.Failure failure = budget.reserve(
            query.requestId(), query.tick(), EmcValue.ZERO, tier
        );
        if (failure != AutomationAuditEvent.Failure.NONE) {
            audit(query, authority, observed, failure);
            return new KnowledgeResult(Collections.unmodifiableSortedSet(new TreeSet<>()), failure);
        }
        SortedSet<EmcKey> available = new TreeSet<>(query.candidates());
        available.retainAll(observed.knowledge());
        audit(query, authority, observed, AutomationAuditEvent.Failure.NONE);
        return new KnowledgeResult(Collections.unmodifiableSortedSet(available),
            AutomationAuditEvent.Failure.NONE);
    }

    public CraftResult craft(TransmutationCraftRequest request, AutomationAuthority authority) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(authority, "authority");
        CraftingTransactionTarget.Snapshot observed = target.snapshot();
        if (!access.permits(authority, AutomationOperation.CRAFT)) {
            return reject(request, authority, observed.account(), EmcValue.ZERO,
                AutomationAuditEvent.Failure.UNAUTHORIZED);
        }
        EmcSnapshot prices = Objects.requireNonNull(emc.get(), "EMC snapshot");
        if (request.emcRevision() != prices.revision()) {
            return reject(request, authority, observed.account(), EmcValue.ZERO,
                AutomationAuditEvent.Failure.STALE_EMC_REVISION);
        }
        Optional<EmcValue> unitValue = prices.find(request.item());
        if (unitValue.isEmpty() || unitValue.orElseThrow().equals(EmcValue.ZERO)) {
            return reject(request, authority, observed.account(), EmcValue.ZERO,
                AutomationAuditEvent.Failure.UNKNOWN_ITEM);
        }
        EmcValue totalValue = unitValue.orElseThrow().multiply(request.count());
        AutomationAuditEvent.Failure failure = budget.reserve(
            request.requestId(), request.tick(), totalValue, tier
        );
        if (failure != AutomationAuditEvent.Failure.NONE) {
            return reject(request, authority, observed.account(), totalValue, failure);
        }
        for (int attempt = 0; attempt < MAX_COMMIT_ATTEMPTS; attempt++) {
            CraftingTransactionTarget.Snapshot before = target.snapshot();
            if (!before.account().knows(request.item())) {
                return reject(request, authority, before.account(), totalValue,
                    AutomationAuditEvent.Failure.UNKNOWN_ITEM);
            }
            Optional<PlayerAlchemyState> debited = before.account().debit(totalValue);
            if (debited.isEmpty()) {
                return reject(request, authority, before.account(), totalValue,
                    AutomationAuditEvent.Failure.INSUFFICIENT_EMC);
            }
            CraftingTransactionTarget.CommitResult committed = target.commit(
                before, debited.orElseThrow(), request.item(), request.count()
            );
            if (committed == CraftingTransactionTarget.CommitResult.COMMITTED) {
                AutomationAuditEvent event = event(request, authority, totalValue, totalValue,
                    before.account().balance(), debited.orElseThrow().balance(),
                    AutomationAuditEvent.Failure.NONE);
                audit.record(event);
                return new CraftResult(request.count(), totalValue, event.failure());
            }
            if (committed == CraftingTransactionTarget.CommitResult.OUTPUT_REJECTED) {
                return reject(request, authority, before.account(), totalValue,
                    AutomationAuditEvent.Failure.OUTPUT_REJECTED);
            }
        }
        return reject(request, authority, target.snapshot().account(), totalValue,
            AutomationAuditEvent.Failure.CONTENTION);
    }

    private void audit(KnowledgeQuery query, AutomationAuthority authority, PlayerAlchemyState state,
                       AutomationAuditEvent.Failure failure) {
        audit.record(new AutomationAuditEvent(query.requestId(), query.tick(),
            AutomationOperation.ENUMERATE_KNOWLEDGE, authority.actor(), Optional.empty(), EmcValue.ZERO,
            EmcValue.ZERO, state.balance(), state.balance(), failure));
    }

    private CraftResult reject(TransmutationCraftRequest request, AutomationAuthority authority,
                               PlayerAlchemyState state, EmcValue requested,
                               AutomationAuditEvent.Failure failure) {
        audit.record(event(request, authority, requested, EmcValue.ZERO,
            state.balance(), state.balance(), failure));
        return new CraftResult(0, EmcValue.ZERO, failure);
    }

    private static AutomationAuditEvent event(TransmutationCraftRequest request,
                                               AutomationAuthority authority, EmcValue requested,
                                               EmcValue transferred,
                                               EmcValue before, EmcValue after,
                                               AutomationAuditEvent.Failure failure) {
        return new AutomationAuditEvent(request.requestId(), request.tick(), AutomationOperation.CRAFT,
            authority.actor(), Optional.of(request.item()), requested, transferred, before, after,
            failure);
    }

    public record KnowledgeResult(SortedSet<EmcKey> available, AutomationAuditEvent.Failure failure) {
        public KnowledgeResult {
            Objects.requireNonNull(available, "available");
            Objects.requireNonNull(failure, "failure");
            available = Collections.unmodifiableSortedSet(new TreeSet<>(available));
        }

        public boolean successful() {
            return failure == AutomationAuditEvent.Failure.NONE;
        }
    }

    public record CraftResult(int crafted, EmcValue spent, AutomationAuditEvent.Failure failure) {
        public CraftResult {
            Objects.requireNonNull(spent, "spent");
            Objects.requireNonNull(failure, "failure");
            if (crafted < 0) {
                throw new IllegalArgumentException("Crafted count cannot be negative");
            }
        }

        public boolean successful() {
            return failure == AutomationAuditEvent.Failure.NONE;
        }
    }
}
