package io.github.tufkan1.projectex.api.emc;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** Thread-safe EMC snapshot store. Reloaders replace the complete snapshot atomically. */
public final class EmcValueRegistry {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private Map<EmcKey, EmcValue> values = Map.of();

    public Optional<EmcValue> find(EmcKey key) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(values.get(key));
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

    public Map<EmcKey, EmcValue> snapshot() {
        lock.readLock().lock();
        try {
            return values;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void replaceAll(Map<EmcKey, EmcValue> replacement) {
        Map<EmcKey, EmcValue> immutable = Collections.unmodifiableMap(new TreeMap<>(replacement));
        lock.writeLock().lock();
        try {
            values = immutable;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
