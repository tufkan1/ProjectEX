package io.github.tufkan1.projectex.alchemy;

import io.github.tufkan1.projectex.api.emc.EmcMatch;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/** Immutable counted inventory used as the atomic transaction boundary. */
public record AlchemyInventory(int capacity, Map<EmcMatch, Integer> contents) {
    public AlchemyInventory {
        if (capacity < 0) {
            throw new IllegalArgumentException("Inventory capacity cannot be negative");
        }
        Objects.requireNonNull(contents, "contents");
        TreeMap<EmcMatch, Integer> copy = new TreeMap<>();
        contents.forEach((match, count) -> {
            Objects.requireNonNull(match, "inventory match");
            Objects.requireNonNull(count, "inventory count");
            if (count <= 0) {
                throw new IllegalArgumentException("Inventory counts must be positive");
            }
            copy.put(match, count);
        });
        contents = Collections.unmodifiableMap(copy);
        if (size(contents) > capacity) {
            throw new IllegalArgumentException("Inventory contents exceed capacity");
        }
    }

    public int count(EmcMatch match) {
        return contents.getOrDefault(match, 0);
    }

    public int size() {
        return size(contents);
    }

    public Optional<AlchemyInventory> remove(EmcMatch match, int count) {
        if (count <= 0 || count(match) < count) {
            return Optional.empty();
        }
        TreeMap<EmcMatch, Integer> changed = new TreeMap<>(contents);
        int remaining = changed.get(match) - count;
        if (remaining == 0) {
            changed.remove(match);
        } else {
            changed.put(match, remaining);
        }
        return Optional.of(new AlchemyInventory(capacity, changed));
    }

    public Optional<AlchemyInventory> add(EmcMatch match, int count) {
        if (count <= 0 || (long) size() + count > capacity) {
            return Optional.empty();
        }
        TreeMap<EmcMatch, Integer> changed = new TreeMap<>(contents);
        changed.merge(match, count, Math::addExact);
        return Optional.of(new AlchemyInventory(capacity, changed));
    }

    private static int size(Map<EmcMatch, Integer> contents) {
        return contents.values().stream().reduce(0, Math::addExact);
    }
}
