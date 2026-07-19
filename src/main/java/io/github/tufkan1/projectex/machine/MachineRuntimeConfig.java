package io.github.tufkan1.projectex.machine;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.math.BigInteger;

/** Atomically loaded JVM overrides for machine rates and global network budgets. */
public final class MachineRuntimeConfig {
    public static final String MAX_TRANSFERS_PROPERTY =
        "projectex.machine.maxTransfersPerWorldTick";
    public static final String MAX_EMC_PROPERTY =
        "projectex.machine.maxEmcPerWorldTick";
    public static final String COMPACT_SUN_MULTIPLIER_PROPERTY =
        "projectex.machine.compactSunMultiplier";
    public static final String COLLECTOR_RATE_MULTIPLIER_PROPERTY =
        "projectex.machine.collectorRateMultiplier";
    public static final String RELAY_TRANSFER_MULTIPLIER_PROPERTY =
        "projectex.machine.relayTransferMultiplier";
    public static final String POWER_FLOWER_RATE_MULTIPLIER_PROPERTY =
        "projectex.machine.powerFlowerRateMultiplier";
    private static final int DEFAULT_MAX_TRANSFERS = 65_536;
    private static final EmcValue DEFAULT_MAX_EMC = new EmcValue(BigInteger.ONE.shiftLeft(256));
    private static volatile Settings settings = loadSettings();

    private MachineRuntimeConfig() {
    }

    public static MachineTickBudget networkBudget() {
        return settings.networkBudget;
    }

    public static int compactSunMultiplier() {
        return settings.compactSunMultiplier;
    }

    public static FixedPointRate generationRate(MachineTier tier) {
        return switch (tier.type()) {
            case COLLECTOR -> settings.collectorRate.apply(tier.fixedRate());
            case POWER_FLOWER -> settings.powerFlowerRate.apply(tier.fixedRate());
            case RELAY -> tier.fixedRate();
        };
    }

    public static EmcValue transferLimit(MachineTier tier) {
        EmcValue scaled = switch (tier.type()) {
            case COLLECTOR -> settings.collectorRate.applyFloor(tier.rate());
            case RELAY -> settings.relayTransfer.applyFloor(tier.rate());
            case POWER_FLOWER -> settings.powerFlowerRate.applyFloor(tier.rate());
        };
        return scaled.equals(EmcValue.ZERO) ? EmcValue.of(1) : scaled;
    }

    public static void reload() {
        settings = loadSettings();
    }

    private static Settings loadSettings() {
        return new Settings(
            loadBudget(),
            loadCompactSunMultiplier(),
            MachineRateMultiplier.parse(System.getProperty(COLLECTOR_RATE_MULTIPLIER_PROPERTY)),
            MachineRateMultiplier.parse(System.getProperty(RELAY_TRANSFER_MULTIPLIER_PROPERTY)),
            MachineRateMultiplier.parse(System.getProperty(POWER_FLOWER_RATE_MULTIPLIER_PROPERTY))
        );
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

    private static int loadCompactSunMultiplier() {
        String value = System.getProperty(COMPACT_SUN_MULTIPLIER_PROPERTY);
        if (value == null || value.isBlank()) {
            return 10;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0 || parsed > 1_000_000) {
                throw new IllegalArgumentException("Compact Sun multiplier must be between 0 and 1000000");
            }
            return parsed == 0 ? 1 : parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid Compact Sun multiplier", exception);
        }
    }

    private record Settings(
        MachineTickBudget networkBudget,
        int compactSunMultiplier,
        MachineRateMultiplier collectorRate,
        MachineRateMultiplier relayTransfer,
        MachineRateMultiplier powerFlowerRate
    ) {
    }
}
