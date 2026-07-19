package io.github.tufkan1.projectex.client;

import io.github.tufkan1.projectex.network.EmcTooltipSyncPayload;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** Immutable client cache populated exclusively by the connected server. */
public final class ClientEmcTooltipState {
    private volatile Snapshot snapshot = new Snapshot(-1, Map.of());

    public boolean accept(EmcTooltipSyncPayload payload) {
        if (!payload.isStructurallyValid() || payload.revision() < snapshot.revision()) return false;
        TreeMap<String, String> values = new TreeMap<>();
        for (EmcTooltipSyncPayload.Entry entry : payload.entries()) {
            if (values.put(entry.itemId(), entry.emc()) != null) return false;
        }
        snapshot = new Snapshot(payload.revision(), values);
        return true;
    }

    public Optional<String> find(String itemId) { return Optional.ofNullable(snapshot.values().get(itemId)); }
    public void clear() { snapshot = new Snapshot(-1, Map.of()); }
    public Snapshot snapshot() { return snapshot; }

    public record Snapshot(long revision, Map<String, String> values) {
        public Snapshot { values = Map.copyOf(values); }
    }
}
