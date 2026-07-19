package io.github.tufkan1.projectex.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcMatch;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.internal.emc.EmcValueRegistry;
import io.github.tufkan1.projectex.internal.player.PlayerAlchemySavedData;
import io.github.tufkan1.projectex.machine.MachineBuffer;
import io.github.tufkan1.projectex.machine.MachineNetworkTick;
import io.github.tufkan1.projectex.machine.MachineTickBudget;
import io.github.tufkan1.projectex.player.PlayerAlchemyState;
import io.github.tufkan1.projectex.player.PlayerAlchemyStateCodec;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;

class ReleasePerformanceAuditTest {
    @Test
    void maximumSupportedMachineNetworkSoakMeetsCpuAndAllocationBudgets(TestReporter reporter) {
        long allocatedBefore = allocatedBytes();
        long cpuBefore = cpuNanos();
        String[] targets = new String[ReleaseAuditBudgets.SUPPORTED_MACHINES_PER_LEVEL];
        MachineBuffer[] buffers = new MachineBuffer[targets.length];
        for (int index = 0; index < targets.length; index++) {
            targets[index] = "target-" + index;
            buffers[index] = new MachineBuffer(EmcValue.of(1), EmcValue.ZERO);
        }
        MachineBuffer source = new MachineBuffer(EmcValue.of(2), EmcValue.of(1));
        long moved = 0;
        for (int tickIndex = 0; tickIndex < ReleaseAuditBudgets.MACHINE_SOAK_TICKS; tickIndex++) {
            MachineNetworkTick tick = new MachineNetworkTick(new MachineTickBudget(
                targets.length, EmcValue.of(targets.length)));
            for (int index = 0; index < targets.length; index++) {
                moved += tick.route("source", source, targets[index], buffers[index], EmcValue.of(1))
                    .moved().amount().longValueExact();
                source.insert(buffers[index].extract(EmcValue.of(1)));
            }
            assertEquals(EmcValue.of(1), source.stored());
        }
        long cpuMillis = (cpuNanos() - cpuBefore) / 1_000_000;
        long allocated = allocatedBytes() - allocatedBefore;
        assertEquals((long) targets.length * ReleaseAuditBudgets.MACHINE_SOAK_TICKS, moved);
        assertTrue(cpuMillis <= ReleaseAuditBudgets.MAX_AUDIT_CPU_MILLIS,
            "Machine soak CPU budget exceeded: " + cpuMillis + " ms");
        if (allocatedBefore >= 0) {
            assertTrue(allocated <= ReleaseAuditBudgets.MAX_AUDIT_ALLOCATED_BYTES,
                "Machine soak allocation budget exceeded: " + allocated + " bytes");
        }
        reporter.publishEntry(Map.of("cpuMillis", Long.toString(cpuMillis),
            "allocatedBytes", Long.toString(Math.max(-1, allocated)), "transfers", Long.toString(moved)));
    }

    @Test
    void maximumSupportedPlayerSaveRoundTripsInsideSizeBudget(TestReporter reporter) {
        TreeSet<EmcKey> knowledge = new TreeSet<>();
        for (int index = 0; index < 256; index++) knowledge.add(EmcKey.parse("audit:item_" + index));
        TreeMap<UUID, PlayerAlchemyState> players = new TreeMap<>();
        for (int index = 0; index < ReleaseAuditBudgets.SUPPORTED_PLAYERS; index++) {
            players.put(new UUID(0, index + 1L), new PlayerAlchemyState(EmcValue.of(index), knowledge));
        }
        String encoded = PlayerAlchemyStateCodec.encode(players);
        int bytes = encoded.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        assertTrue(bytes <= ReleaseAuditBudgets.MAX_PLAYER_SAVE_BYTES);
        assertEquals(players, PlayerAlchemyStateCodec.decode(encoded));
        reporter.publishEntry("playerSaveBytes", Integer.toString(bytes));
    }

    @Test
    void concurrentAutomationAndReloadPublicationNeverDuplicateOrExposePartialState() throws Exception {
        PlayerAlchemySavedData players = new PlayerAlchemySavedData();
        UUID player = new UUID(0, 1);
        try (var executor = Executors.newFixedThreadPool(8)) {
            var work = new java.util.ArrayList<java.util.concurrent.Callable<Void>>();
            for (int thread = 0; thread < 8; thread++) work.add(() -> {
                for (int attempt = 0; attempt < 1_000; attempt++) {
                    while (true) {
                        PlayerAlchemyState before = players.state(player);
                        long revision = players.revision(player);
                        if (players.compareAndSet(player, before, revision, before.credit(EmcValue.of(1)))) break;
                    }
                }
                return null;
            });
            for (var result : executor.invokeAll(work)) result.get();
        }
        assertEquals(EmcValue.of(8_000), players.state(player).balance());

        EmcValueRegistry registry = new EmcValueRegistry();
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicBoolean inconsistent = new AtomicBoolean(false);
        try (var readers = Executors.newFixedThreadPool(4)) {
            for (int index = 0; index < 4; index++) readers.submit(() -> {
                while (running.get()) {
                    var snapshot = registry.snapshot();
                    if (!snapshot.values().keySet().equals(snapshot.sources().keySet())) inconsistent.set(true);
                }
            });
            EmcMatch key = EmcMatch.item(EmcKey.parse("minecraft:coal"));
            for (int revision = 1; revision <= 2_000; revision++) {
                registry.replaceAll(Map.of(key, EmcValue.of(revision)), Map.of(key, "audit"));
            }
            running.set(false);
        }
        assertTrue(!inconsistent.get());
        assertEquals(2_000, registry.snapshot().find(EmcKey.parse("minecraft:coal"))
            .orElseThrow().amount().intValueExact());
    }

    private static long cpuNanos() {
        var bean = ManagementFactory.getThreadMXBean();
        return bean.isCurrentThreadCpuTimeSupported() ? bean.getCurrentThreadCpuTime() : System.nanoTime();
    }

    private static long allocatedBytes() {
        var bean = ManagementFactory.getThreadMXBean();
        if (!(bean instanceof com.sun.management.ThreadMXBean allocation) || !allocation.isThreadAllocatedMemorySupported()) {
            return -1;
        }
        if (!allocation.isThreadAllocatedMemoryEnabled()) allocation.setThreadAllocatedMemoryEnabled(true);
        return allocation.getThreadAllocatedBytes(Thread.currentThread().threadId());
    }
}
