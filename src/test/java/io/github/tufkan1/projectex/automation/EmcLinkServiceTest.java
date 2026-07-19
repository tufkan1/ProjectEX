package io.github.tufkan1.projectex.automation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.machine.ExpansionMachineTier;
import io.github.tufkan1.projectex.player.PlayerAlchemyState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

final class EmcLinkServiceTest {
    private static final EmcKey COAL = EmcKey.parse("minecraft:coal");
    private static final EmcKey DIAMOND = EmcKey.parse("minecraft:diamond");

    @Test
    void accessPolicyNeverTurnsPublicInsertIntoExtractionOrKnowledgeAccess() {
        UUID owner = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        AutomationAccess access = AutomationAccess.ownedBy(owner)
            .withMember(member, true, AutomationAuthority.online(owner, false))
            .withPublicInsert(true, AutomationAuthority.online(owner, false));

        assertTrue(access.permits(AutomationAuthority.online(member, false), AutomationOperation.EXTRACT_EMC));
        assertTrue(access.permits(AutomationAuthority.online(stranger, false), AutomationOperation.INSERT_EMC));
        assertFalse(access.permits(AutomationAuthority.online(stranger, false), AutomationOperation.EXTRACT_EMC));
        assertFalse(access.permits(AutomationAuthority.online(stranger, false), AutomationOperation.ENUMERATE_KNOWLEDGE));
        assertTrue(access.permits(AutomationAuthority.machine(), AutomationOperation.EXTRACT_EMC));
        assertThrows(IllegalArgumentException.class,
            () -> new AutomationAuthority(Optional.empty(), true, false));
        assertThrows(SecurityException.class,
            () -> access.withMember(UUID.randomUUID(), true, AutomationAuthority.online(stranger, false)));
    }

    @Test
    void strictAccessCodecRoundTripsAndRejectsIdentityCorruption() {
        UUID owner = UUID.randomUUID();
        AutomationAccess access = AutomationAccess.ownedBy(owner)
            .withMember(UUID.randomUUID(), true, AutomationAuthority.online(owner, false))
            .withPublicInsert(true, AutomationAuthority.online(owner, false));
        assertEquals(access, AutomationAccessCodec.decode(AutomationAccessCodec.encode(access)));

        Map<String, String> wrongVersion = new java.util.HashMap<>(AutomationAccessCodec.encode(access));
        wrongVersion.put("version", "2");
        assertThrows(IllegalArgumentException.class, () -> AutomationAccessCodec.decode(wrongVersion));
        Map<String, String> ownerAsMember = new java.util.HashMap<>(AutomationAccessCodec.encode(access));
        ownerAsMember.put("members", owner.toString());
        assertThrows(IllegalArgumentException.class, () -> AutomationAccessCodec.decode(ownerAsMember));
        Map<String, String> unknownField = new java.util.HashMap<>(AutomationAccessCodec.encode(access));
        unknownField.put("surprise", "true");
        assertThrows(IllegalArgumentException.class, () -> AutomationAccessCodec.decode(unknownField));
    }

    @Test
    void filtersBudgetsReplayAndAuditRejectBeforeMutation() {
        UUID owner = UUID.randomUUID();
        InMemoryAccount account = new InMemoryAccount(state(100));
        List<AutomationAuditEvent> events = new ArrayList<>();
        EmcLinkService service = new EmcLinkService(
            EmcAutomationTier.of(ExpansionMachineTier.BASIC),
            AutomationAccess.ownedBy(owner),
            new EmcLinkFilter(EmcLinkFilter.Mode.ALLOW_LIST, new TreeSet<>(java.util.Set.of(COAL))),
            EmcLinkFilter.allowAll(), account, events::add
        );
        AutomationAuthority machine = AutomationAuthority.machine();
        UUID requestId = UUID.randomUUID();

        assertFalse(service.transfer(request(requestId, 1, AutomationOperation.INSERT_EMC, 10, DIAMOND), machine)
            .successful());
        assertEquals(EmcValue.of(100), account.snapshot().balance());
        assertTrue(service.transfer(request(requestId, 1, AutomationOperation.INSERT_EMC, 10, COAL), machine)
            .successful());
        assertFalse(service.transfer(request(requestId, 1, AutomationOperation.INSERT_EMC, 10, COAL), machine)
            .successful());
        assertEquals(AutomationAuditEvent.Failure.STALE_TICK,
            service.transfer(request(UUID.randomUUID(), 0, AutomationOperation.INSERT_EMC, 10, COAL), machine)
                .failure());
        assertFalse(service.transfer(request(UUID.randomUUID(), 1, AutomationOperation.EXTRACT_EMC, 65, COAL), machine)
            .successful());
        assertEquals(AutomationAuditEvent.Failure.REQUEST_LIMIT, events.get(events.size() - 1).failure());
        assertEquals(EmcValue.of(110), account.snapshot().balance());
    }

    @Test
    void concurrentLinksCannotOverspendSharedAccount() throws Exception {
        UUID owner = UUID.randomUUID();
        InMemoryAccount account = new InMemoryAccount(state(100));
        List<AutomationAuditEvent> events = java.util.Collections.synchronizedList(new ArrayList<>());
        EmcLinkService first = service(owner, account, events);
        EmcLinkService second = service(owner, account, events);
        List<Callable<EmcLinkService.Transfer>> work = new ArrayList<>();
        for (int index = 0; index < 20; index++) {
            EmcLinkService selected = index % 2 == 0 ? first : second;
            int request = index;
            work.add(() -> selected.transfer(
                request(new UUID(0, request + 1L), 5, AutomationOperation.EXTRACT_EMC, 10, COAL),
                AutomationAuthority.machine()
            ));
        }
        try (var executor = Executors.newFixedThreadPool(8)) {
            long successful = executor.invokeAll(work).stream().map(future -> {
                try {
                    return future.get();
                } catch (Exception exception) {
                    throw new AssertionError(exception);
                }
            }).filter(EmcLinkService.Transfer::successful).count();
            assertEquals(10, successful);
        }
        assertEquals(EmcValue.ZERO, account.snapshot().balance());
        assertEquals(20, events.size());
        assertEquals(EmcValue.of(100), events.stream().map(AutomationAuditEvent::transferred)
            .reduce(EmcValue.ZERO, EmcValue::add));
    }

    private static EmcLinkService service(
        UUID owner,
        InMemoryAccount account,
        List<AutomationAuditEvent> events
    ) {
        return new EmcLinkService(
            EmcAutomationTier.of(ExpansionMachineTier.BASIC), AutomationAccess.ownedBy(owner),
            EmcLinkFilter.allowAll(), EmcLinkFilter.allowAll(), account, events::add
        );
    }

    private static EmcLinkRequest request(
        UUID id, long tick, AutomationOperation operation, long amount, EmcKey item
    ) {
        return new EmcLinkRequest(id, tick, operation, EmcValue.of(amount), Optional.of(item));
    }

    private static PlayerAlchemyState state(long balance) {
        return new PlayerAlchemyState(EmcValue.of(balance), new TreeSet<>());
    }

    private static final class InMemoryAccount implements AutomationAccount {
        private PlayerAlchemyState state;

        private InMemoryAccount(PlayerAlchemyState state) {
            this.state = state;
        }

        @Override
        public synchronized PlayerAlchemyState snapshot() {
            return state;
        }

        @Override
        public synchronized boolean compareAndSet(PlayerAlchemyState expected, PlayerAlchemyState replacement) {
            if (!state.equals(expected)) {
                return false;
            }
            state = replacement;
            return true;
        }
    }
}
