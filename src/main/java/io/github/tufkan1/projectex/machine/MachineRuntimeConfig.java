package io.github.tufkan1.projectex.machine;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.math.BigInteger;

/** Validated server JVM overrides for global machine-network tick budgets. */
public final class MachineRuntimeConfig {
    public static final String MAX_TRANSFERS_PROPERTY =
        "projectex.machine.maxTransfersPerWorldTick";
    public static final String MAX_EMC_PROPERTY =
        "projectex.machine.maxEmcPerWorldTick";
    private static final int DEFAULT_MAX_TRANSFERS = 65_536;
    private static final EmcValue DEFAULT_MAX_EMC = new EmcValue(BigInteger.ONE.shiftLeft(256));
    private static volatile MachineTickBudget networkBudget = loadBudget();

    private MachineRuntimeConfig() {
    }

    public static MachineTickBudget networkBudget() {
        return networkBudget;
    }

    public static void reload() {
        networkBudget = loadBudget();
    }

    private static MachineTickBudget loadBudget() {
        int transfers = parseTransfers(System.getProperty(MAX_TRANSFERS_PROPERTY));
        EmcValue emc = parseEmc(System.getProperty(MAX_EMC_PROPERTY));
        return new MachineTickBudget(transfers, emc);
    }

    private static int parseTransfers(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_MAX_TRANSFERS;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 1 || parsed > 1_000_000) {
                throw new IllegalArgumentException("Machine transfer budget must be between 1 and 1000000");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid machine transfer budget", exception);
        }
    }

    private static EmcValue parseEmc(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_MAX_EMC;
        }
        if (value.length() > 4096 || !value.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("Invalid machine EMC tick budget");
        }
        EmcValue parsed = new EmcValue(new BigInteger(value));
        if (parsed.equals(EmcValue.ZERO)) {
            throw new IllegalArgumentException("Machine EMC tick budget must be positive");
        }
        return parsed;
    }
}
