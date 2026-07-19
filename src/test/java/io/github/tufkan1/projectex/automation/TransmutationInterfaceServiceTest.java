package io.github.tufkan1.projectex.automation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcMatch;
import io.github.tufkan1.projectex.api.emc.EmcSnapshot;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.machine.ExpansionMachineTier;
import io.github.tufkan1.projectex.player.PlayerAlchemyState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

final class TransmutationInterfaceServiceTest {
    private static final EmcKey COAL = EmcKey.parse("minecraft:coal");
    private static final EmcKey DIAMOND = EmcKey.parse("minecraft:diamond");

    @Test
    void unauthorizedCallersCannotDiscoverAnyKnowledge() {
        UUID owner = UUID.randomUUID();
        AtomicTarget target = new AtomicTarget(state(100, COAL, DIAMOND), 64);
        List<AutomationAuditEvent> events = new ArrayList<>();
        TransmutationInterfaceService service = service(owner, target, events);

        var result = service.available(new KnowledgeQuery(UUID.randomUUID(), 1,
            new TreeSet<>(Set.of(COAL, DIAMOND))),
            AutomationAuthority.online(UUID.randomUUID(), false));

        assertFalse(result.successful());
        assertTrue(result.available().isEmpty());
        assertEquals(AutomationAuditEvent.Failure.UNAUTHORIZED, result.failure());
        assertEquals(AutomationOperation.ENUMERATE_KNOWLEDGE, events.getFirst().operation());
    }

    @Test
    void availabilityOnlyIntersectsBoundedCallerCandidates() {
        UUID owner = UUID.randomUUID();
        AtomicTarget target = new AtomicTarget(state(100, COAL), 64);
        TransmutationInterfaceService service = service(owner, target, new ArrayList<>());

        var result = service.available(new KnowledgeQuery(UUID.randomUUID(), 1,
            new TreeSet<>(Set.of(COAL, DIAMOND))), AutomationAuthority.online(owner, false));

        assertTrue(result.successful());
        assertEquals(new TreeSet<>(Set.of(COAL)), result.available());
    }

    @Test
    void rejectedOutputAndUnknownKnowledgeNeverDebitAccount() {
        UUID owner = UUID.randomUUID();
        AtomicTarget full = new AtomicTarget(state(100, COAL), 0);
        TransmutationInterfaceService service = service(owner, full, new ArrayList<>());

        var rejected = service.craft(request(1, COAL), AutomationAuthority.machine());
        assertEquals(AutomationAuditEvent.Failure.OUTPUT_REJECTED, rejected.failure());
        assertEquals(EmcValue.of(100), full.snapshot().account().balance());

        var unknown = service.craft(request(2, DIAMOND), AutomationAuthority.machine());
        assertEquals(AutomationAuditEvent.Failure.UNKNOWN_ITEM, unknown.failure());
        assertEquals(EmcValue.of(100), full.snapshot().account().balance());
        assertEquals(0, full.inserted());
    }

    @Test
    void staleServerPriceRevisionFailsBeforeBudgetOrMutation() {
        UUID owner = UUID.randomUUID();
        AtomicTarget target = new AtomicTarget(state(100, COAL), 64);
        List<AutomationAuditEvent> events = new ArrayList<>();
        TransmutationInterfaceService service = service(owner, target, events);

        var stale = service.craft(new TransmutationCraftRequest(
            UUID.randomUUID(), 1, COAL, 0, 1
        ), AutomationAuthority.machine());

        assertEquals(AutomationAuditEvent.Failure.STALE_EMC_REVISION, stale.failure());
        assertEquals(EmcValue.of(100), target.snapshot().account().balance());
        assertEquals(0, target.inserted());
        assertEquals(EmcValue.ZERO, events.getFirst().requested());
    }

    @Test
    void concurrentInterfacesAtomicallyPreventOverspendAndDuplication() throws Exception {
        UUID owner = UUID.randomUUID();
        AtomicTarget target = new AtomicTarget(state(100, COAL), 64);
        List<AutomationAuditEvent> events = java.util.Collections.synchronizedList(new ArrayList<>());
        TransmutationInterfaceService first = service(owner, target, events);
        TransmutationInterfaceService second = service(owner, target, events);
        List<Callable<TransmutationInterfaceService.CraftResult>> work = new ArrayList<>();
        for (int index = 0; index < 20; index++) {
            TransmutationInterfaceService selected = index % 2 == 0 ? first : second;
            int request = index;
            work.add(() -> selected.craft(new TransmutationCraftRequest(
                new UUID(1, request + 1L), 5, COAL, 1, 1
            ), AutomationAuthority.machine()));
        }
        long successful;
        try (var executor = Executors.newFixedThreadPool(8)) {
            successful = executor.invokeAll(work).stream().filter(future -> {
                try {
                    return future.get().successful();
                } catch (Exception exception) {
                    throw new AssertionError(exception);
                }
            }).count();
        }

        assertEquals(10, successful);
        assertEquals(10, target.inserted());
        assertEquals(EmcValue.ZERO, target.snapshot().account().balance());
        assertEquals(20, events.size());
    }

    private static TransmutationCraftRequest request(long id, EmcKey item) {
        return new TransmutationCraftRequest(new UUID(2, id), 1, item, 1, 1);
    }

    private static TransmutationInterfaceService service(
        UUID owner, AtomicTarget target, List<AutomationAuditEvent> events
    ) {
        return new TransmutationInterfaceService(EmcAutomationTier.of(ExpansionMachineTier.BASIC),
            AutomationAccess.ownedBy(owner), target, () -> prices(), events::add);
    }

    private static EmcSnapshot prices() {
        Map<EmcMatch, EmcValue> values = Map.of(
            EmcMatch.item(COAL), EmcValue.of(10),
            EmcMatch.item(DIAMOND), EmcValue.of(40)
        );
        return new EmcSnapshot(1, values, Map.of(
            EmcMatch.item(COAL), "test",
            EmcMatch.item(DIAMOND), "test"
        ));
    }

    private static PlayerAlchemyState state(long balance, EmcKey... knowledge) {
        return new PlayerAlchemyState(EmcValue.of(balance), new TreeSet<>(Set.of(knowledge)));
    }

    private static final class AtomicTarget implements CraftingTransactionTarget {
        private PlayerAlchemyState state;
        private long revision;
        private int capacity;
        private int inserted;

        private AtomicTarget(PlayerAlchemyState state, int capacity) {
            this.state = state;
            this.capacity = capacity;
        }

        @Override
        public synchronized Snapshot snapshot() {
            return new Snapshot(state, revision);
        }

        @Override
        public synchronized CommitResult commit(
            Snapshot expected, PlayerAlchemyState replacement, EmcKey item, int count
        ) {
            if (expected.revision() != revision || !expected.account().equals(state)) {
                return CommitResult.CONTENTION;
            }
            if (count > capacity) {
                return CommitResult.OUTPUT_REJECTED;
            }
            state = replacement;
            revision++;
            capacity -= count;
            inserted += count;
            return CommitResult.COMMITTED;
        }

        private synchronized int inserted() {
            return inserted;
        }
    }
}
