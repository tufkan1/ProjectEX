package io.github.tufkan1.projectex.machine;

/** Server-side redstone activation policy persisted by machines. */
public enum MachineRedstoneMode {
    IGNORED,
    REQUIRE_SIGNAL,
    REQUIRE_NO_SIGNAL;

    public boolean enabled(boolean powered) {
        return switch (this) {
            case IGNORED -> true;
            case REQUIRE_SIGNAL -> powered;
            case REQUIRE_NO_SIGNAL -> !powered;
        };
    }
}
