package io.github.tufkan1.projectex.machine;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.util.Objects;

/** Exact bounded EMC buffer; mutations never clamp corrupt input silently. */
public final class MachineBuffer {
    private final EmcValue capacity;
    private EmcValue stored;

    public MachineBuffer(EmcValue capacity, EmcValue stored) {
        this.capacity = Objects.requireNonNull(capacity, "capacity");
        this.stored = Objects.requireNonNull(stored, "stored");
        if (stored.compareTo(capacity) > 0) {
            throw new IllegalArgumentException("Stored EMC exceeds machine capacity");
        }
    }

    public EmcValue capacity() {
        return capacity;
    }

    public EmcValue stored() {
        return stored;
    }

    public EmcValue insert(EmcValue requested) {
        EmcValue accepted = requested.min(capacity.subtract(stored));
        stored = stored.add(accepted);
        return accepted;
    }

    public EmcValue extract(EmcValue requested) {
        EmcValue extracted = requested.min(stored);
        stored = stored.subtract(extracted);
        return extracted;
    }

    public int comparatorSignal() {
        if (capacity.equals(EmcValue.ZERO)) {
            return 0;
        }
        return stored.amount().multiply(java.math.BigInteger.valueOf(15))
            .divide(capacity.amount()).intValue();
    }
}
