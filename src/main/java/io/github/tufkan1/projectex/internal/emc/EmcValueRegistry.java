package io.github.tufkan1.projectex.internal.emc;

import io.github.tufkan1.projectex.api.emc.EmcApi;
import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcMatch;
import io.github.tufkan1.projectex.api.emc.EmcReloadListener;
import io.github.tufkan1.projectex.api.emc.EmcSnapshot;
import io.github.tufkan1.projectex.api.emc.EmcSubscription;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Internal mutable publisher behind the query-only public API. */
public final class EmcValueRegistry implements EmcApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmcValueRegistry.class);

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final CopyOnWriteArrayList<EmcReloadListener> listeners = new CopyOnWriteArrayList<>();
    private EmcSnapshot snapshot = new EmcSnapshot(0, Map.of(), Map.of());
    private EmcSnapshot stagedSnapshot;

    @Override
    public Optional<EmcValue> find(EmcKey key) {
        return snapshot().find(key);
    }

    @Override
    public Optional<EmcValue> find(EmcMatch match) {
        return snapshot().find(match);
    }

    @Override
    public EmcSnapshot snapshot() {
        lock.readLock().lock();
        try {
            return snapshot;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public EmcSubscription subscribe(EmcReloadListener listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    /** Stages reload output without exposing a partial pre-recipe generation to API consumers. */
    public void stageAll(Map<EmcMatch, EmcValue> values, Map<EmcMatch, String> sources) {
        EmcSnapshot staged = new EmcSnapshot(snapshot().revision(), values, sources);
        lock.writeLock().lock();
        try {
            stagedSnapshot = staged;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Returns staged explicit data for the server-side recipe mapping phase. */
    public EmcSnapshot stagedSnapshot() {
        lock.readLock().lock();
        try {
            return stagedSnapshot == null ? snapshot : stagedSnapshot;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void replaceAll(Map<EmcMatch, EmcValue> values, Map<EmcMatch, String> sources) {
        EmcSnapshot published;
        lock.writeLock().lock();
        try {
            published = new EmcSnapshot(snapshot.revision() + 1, values, sources);
            snapshot = published;
            stagedSnapshot = null;
        } finally {
            lock.writeLock().unlock();
        }
        listeners.forEach(listener -> notifyListener(listener, published));
    }

    private static void notifyListener(EmcReloadListener listener, EmcSnapshot published) {
        try {
            listener.onReload(published);
        } catch (RuntimeException exception) {
            LOGGER.error("EMC reload listener failed at revision {}", published.revision(), exception);
        }
    }
}
