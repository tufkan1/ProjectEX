package io.github.tufkan1.projectex.player;

import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

/** Immutable persistent balance and learned-item knowledge for one player. */
public record PlayerAlchemyState(EmcValue balance, SortedSet<EmcKey> knowledge) {
    public static final int MAX_BALANCE_DIGITS = 4096;
    public static final int MAX_KNOWLEDGE_ENTRIES = 100_000;
    public static final PlayerAlchemyState EMPTY = new PlayerAlchemyState(EmcValue.ZERO, new TreeSet<>());

    public PlayerAlchemyState {
        Objects.requireNonNull(balance, "balance");
        Objects.requireNonNull(knowledge, "knowledge");
        if (balance.amount().toString().length() > MAX_BALANCE_DIGITS) {
            throw new IllegalArgumentException("Player EMC balance exceeds " + MAX_BALANCE_DIGITS + " digits");
        }
        if (knowledge.size() > MAX_KNOWLEDGE_ENTRIES) {
            throw new IllegalArgumentException("Player knowledge exceeds " + MAX_KNOWLEDGE_ENTRIES + " entries");
        }
        knowledge = Collections.unmodifiableSortedSet(new TreeSet<>(knowledge));
    }

    public boolean knows(EmcKey item) {
        return knowledge.contains(item);
    }

    public PlayerAlchemyState credit(EmcValue amount) {
        Objects.requireNonNull(amount, "amount");
        return new PlayerAlchemyState(balance.add(amount), knowledge);
    }

    public Optional<PlayerAlchemyState> debit(EmcValue amount) {
        Objects.requireNonNull(amount, "amount");
        BigInteger remaining = balance.amount().subtract(amount.amount());
        return remaining.signum() < 0
            ? Optional.empty()
            : Optional.of(new PlayerAlchemyState(new EmcValue(remaining), knowledge));
    }

    public PlayerAlchemyState learn(EmcKey item) {
        Objects.requireNonNull(item, "item");
        if (knowledge.contains(item)) {
            return this;
        }
        SortedSet<EmcKey> learned = new TreeSet<>(knowledge);
        learned.add(item);
        return new PlayerAlchemyState(balance, learned);
    }
}
