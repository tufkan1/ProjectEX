package io.github.tufkan1.projectex.knowledge;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Bounded one-shot registry for confirmed knowledge snapshot identifiers. */
public final class KnowledgeReplayGuard {
    private final int capacity;
    private final LinkedHashMap<UUID, Long> consumed = new LinkedHashMap<>();

    public KnowledgeReplayGuard(int capacity) {
        this(capacity, Map.of());
    }

    public KnowledgeReplayGuard(int capacity, Map<UUID, Long> initial) {
        if (capacity < 1 || capacity > 100_000) throw new IllegalArgumentException("Invalid replay capacity");
        this.capacity = capacity;
        initial.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEach(entry -> {
            if (consumed.size() < capacity) consumed.put(entry.getKey(), entry.getValue());
        });
    }

    public synchronized boolean consume(KnowledgeSnapshot snapshot, Instant now) {
        consumed.entrySet().removeIf(entry -> entry.getValue() <= now.getEpochSecond());
        if (consumed.containsKey(snapshot.snapshotId())) return false;
        while (consumed.size() >= capacity) consumed.remove(consumed.keySet().iterator().next());
        consumed.put(snapshot.snapshotId(), snapshot.expiresAt());
        return true;
    }

    public synchronized int size() { return consumed.size(); }

    public synchronized Map<UUID, Long> snapshot(Instant now) {
        consumed.entrySet().removeIf(entry -> entry.getValue() <= now.getEpochSecond());
        return Map.copyOf(consumed);
    }
}
