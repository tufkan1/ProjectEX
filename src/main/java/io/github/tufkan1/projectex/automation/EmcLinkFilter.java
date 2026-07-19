package io.github.tufkan1.projectex.automation;

import io.github.tufkan1.projectex.api.emc.EmcKey;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

/** Bounded item identity filter applied independently to insert and extract paths. */
public record EmcLinkFilter(Mode mode, SortedSet<EmcKey> items) {
    public EmcLinkFilter {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(items, "items");
        items = Collections.unmodifiableSortedSet(new TreeSet<>(items));
    }

    public static EmcLinkFilter allowAll() {
        return new EmcLinkFilter(Mode.DENY_LIST, new TreeSet<>());
    }

    public boolean permits(Optional<EmcKey> item, int maximumEntries) {
        Objects.requireNonNull(item, "item");
        if (items.size() > maximumEntries) {
            return false;
        }
        if (item.isEmpty()) {
            return mode == Mode.DENY_LIST && items.isEmpty();
        }
        boolean contains = item.filter(items::contains).isPresent();
        return mode == Mode.ALLOW_LIST ? contains : !contains;
    }

    public enum Mode {
        ALLOW_LIST,
        DENY_LIST
    }
}
