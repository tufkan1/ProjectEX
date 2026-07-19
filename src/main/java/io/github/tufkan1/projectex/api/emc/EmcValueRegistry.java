package io.github.tufkan1.projectex.api.emc;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** Thread-safe EMC snapshot store. Reloaders replace the complete snapshot atomically. */
public final class EmcValueRegistry {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private Map<EmcMatch, EmcValue> values = Map.of();

    public Optional<EmcValue> find(EmcKey key) {
        return find(EmcMatch.item(key));
    }

    public Optional<EmcValue> find(EmcMatch match) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(values.get(match));
        } finally {
            lock.readLock().unlock();
        }
    }

    public int size() {
        lock.readLock().lock();
        try {
            return values.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    public Map<EmcMatch, EmcValue> snapshot() {
        lock.readLock().lock();
        try {
            return values;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void replaceAll(Map<EmcMatch, EmcValue> replacement) {
        Map<EmcMatch, EmcValue> immutable = Collections.unmodifiableMap(new TreeMap<>(replacement));
        lock.writeLock().lock();
        try {
            values = immutable;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
