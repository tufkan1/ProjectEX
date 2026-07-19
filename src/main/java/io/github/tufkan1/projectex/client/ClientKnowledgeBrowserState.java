package io.github.tufkan1.projectex.client;

import io.github.tufkan1.projectex.alchemy.AlchemyTransactionFailure;
import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.network.AlchemyKnowledgePagePayload;
import io.github.tufkan1.projectex.network.AlchemyKnowledgeRequestPayload;
import io.github.tufkan1.projectex.network.AlchemyNetworkProtocol;
import java.util.Collections;
import java.util.Collection;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

/** Authoritative page cache plus client-local favorites for the transmutation screen. */
public final class ClientKnowledgeBrowserState {
    public static final int MAX_FAVORITES = 10_000;
    private Snapshot snapshot = Snapshot.closed();
    private final SortedSet<String> favorites = new TreeSet<>();

    public synchronized void open(long sessionId) {
        if (sessionId == 0) {
            throw new IllegalArgumentException("Session id cannot be zero");
        }
        snapshot = Snapshot.open(sessionId, favorites);
    }

    public synchronized Optional<AlchemyKnowledgeRequestPayload> nextQuery(
        String query,
        int page,
        int pageSize
    ) {
        if (!snapshot.active() || snapshot.nextQueryId() == Long.MAX_VALUE) {
            return Optional.empty();
        }
        AlchemyKnowledgeRequestPayload payload;
        try {
            payload = new AlchemyKnowledgeRequestPayload(
                AlchemyNetworkProtocol.VERSION,
                snapshot.sessionId(),
                snapshot.nextQueryId(),
                query,
                page,
                pageSize
            );
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
        if (!payload.hasValidShape()) {
            return Optional.empty();
        }
        snapshot = snapshot.withNextQueryId(snapshot.nextQueryId() + 1);
        return Optional.of(payload);
    }

    public synchronized boolean accept(AlchemyKnowledgePagePayload payload) {
        if (!snapshot.active()
            || !payload.isStructurallyValid()
            || payload.sessionId() != snapshot.sessionId()
            || payload.queryId() >= snapshot.nextQueryId()
            || payload.queryId() <= snapshot.lastResponseId()) {
            return false;
        }
        snapshot = new Snapshot(
            true,
            snapshot.sessionId(),
            snapshot.nextQueryId(),
            payload.queryId(),
            payload.page(),
            payload.totalPages(),
            payload.totalEntries(),
            payload.entries(),
            payload.failure().filter(failure -> failure != AlchemyTransactionFailure.NONE),
            immutableFavorites()
        );
        return true;
    }

    public synchronized boolean toggleFavorite(String itemId) {
        try {
            EmcKey.parse(itemId);
        } catch (IllegalArgumentException exception) {
            return false;
        }
        if (!favorites.remove(itemId)) {
            if (favorites.size() >= MAX_FAVORITES) {
                return false;
            }
            favorites.add(itemId);
        }
        snapshot = snapshot.withFavorites(immutableFavorites());
        return true;
    }

    public synchronized void replaceFavorites(Collection<String> replacement) {
        favorites.clear();
        for (String itemId : replacement) {
            if (favorites.size() == MAX_FAVORITES) {
                break;
            }
            try {
                EmcKey.parse(itemId);
                favorites.add(itemId);
            } catch (IllegalArgumentException ignored) {
                // Preference files may contain stale or manually edited entries.
            }
        }
        snapshot = snapshot.withFavorites(immutableFavorites());
    }

    public synchronized void close() {
        snapshot = Snapshot.closed().withFavorites(immutableFavorites());
    }

    public synchronized Snapshot snapshot() {
        return snapshot;
    }

    private SortedSet<String> immutableFavorites() {
        return Collections.unmodifiableSortedSet(new TreeSet<>(favorites));
    }

    public record Snapshot(
        boolean active,
        long sessionId,
        long nextQueryId,
        long lastResponseId,
        int page,
        int totalPages,
        int totalEntries,
        java.util.List<AlchemyKnowledgePagePayload.Entry> entries,
        Optional<AlchemyTransactionFailure> lastFailure,
        SortedSet<String> favorites
    ) {
        public Snapshot {
            entries = java.util.List.copyOf(entries);
            lastFailure = java.util.Objects.requireNonNull(lastFailure, "lastFailure");
            favorites = Collections.unmodifiableSortedSet(new TreeSet<>(favorites));
        }

        public int visibleFavoriteCount() {
            return (int) entries.stream().filter(entry -> favorites.contains(entry.itemId())).count();
        }

        private static Snapshot open(long sessionId, SortedSet<String> favorites) {
            return new Snapshot(true, sessionId, 0, -1, 0, 0, 0,
                java.util.List.of(), Optional.empty(), favorites);
        }

        private static Snapshot closed() {
            return new Snapshot(false, 0, 0, -1, 0, 0, 0,
                java.util.List.of(), Optional.empty(), new TreeSet<>());
        }

        private Snapshot withNextQueryId(long next) {
            return new Snapshot(active, sessionId, next, lastResponseId, page, totalPages,
                totalEntries, entries, lastFailure, favorites);
        }

        private Snapshot withFavorites(SortedSet<String> replacement) {
            return new Snapshot(active, sessionId, nextQueryId, lastResponseId, page, totalPages,
                totalEntries, entries, lastFailure, replacement);
        }
    }
}
