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
    private Map<EmcMatch, String> sources = Map.of();

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

    public Optional<String> findSource(EmcMatch match) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(sources.get(match));
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

    public Map<EmcMatch, String> sourcesSnapshot() {
        lock.readLock().lock();
        try {
            return sources;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void replaceAll(Map<EmcMatch, EmcValue> replacement) {
        replaceAll(replacement, replacement.keySet().stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(match -> match, ignored -> "unknown")));
    }

    public void replaceAll(Map<EmcMatch, EmcValue> replacement, Map<EmcMatch, String> sourceReplacement) {
        Map<EmcMatch, EmcValue> immutable = Collections.unmodifiableMap(new TreeMap<>(replacement));
        Map<EmcMatch, String> immutableSources = Collections.unmodifiableMap(new TreeMap<>(sourceReplacement));
        if (!immutable.keySet().equals(immutableSources.keySet())) {
            throw new IllegalArgumentException("EMC values and sources must contain identical keys");
        }
        lock.writeLock().lock();
        try {
            values = immutable;
            sources = immutableSources;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
