package io.github.tufkan1.projectex.endgame;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.api.endgame.FinalStarSlot;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/** Validated server JVM gates for endgame capabilities and consumables. */
public final class EndgameRuntimeConfig {
    public static final String FINAL_STAR_ENABLED = "projectex.finalStar.enabled";
    public static final String FINAL_STAR_SLOTS = "projectex.finalStar.slots";
    public static final String FINAL_STAR_COOLDOWN = "projectex.finalStar.cooldownTicks";
    public static final String CONSUMABLES_ENABLED = "projectex.infiniteConsumables.enabled";
    public static final String STEAK_COST = "projectex.infiniteSteak.emcCost";
    public static final String STEAK_COOLDOWN = "projectex.infiniteSteak.cooldownTicks";
    private static volatile Snapshot snapshot = load();

    private EndgameRuntimeConfig() { }
    public static Snapshot snapshot() { return snapshot; }
    public static void reload() { snapshot = load(); }

    static Snapshot load() {
        boolean finalStar = bool(FINAL_STAR_ENABLED, true);
        boolean consumables = bool(CONSUMABLES_ENABLED, true);
        int finalCooldown = integer(FINAL_STAR_COOLDOWN, 20, 1, 72_000);
        int steakCooldown = integer(STEAK_COOLDOWN, 20, 1, 72_000);
        String rawSlots = System.getProperty(FINAL_STAR_SLOTS, "main_hand,off_hand,inventory");
        EnumSet<FinalStarSlot> slots = EnumSet.noneOf(FinalStarSlot.class);
        Arrays.stream(rawSlots.split(",")).map(String::trim).filter(value -> !value.isEmpty())
            .forEach(value -> slots.add(Arrays.stream(FinalStarSlot.values())
                .filter(slot -> slot.serializedName().equals(value)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown Final Star slot: " + value))));
        if (slots.isEmpty()) throw new IllegalArgumentException("Final Star slots cannot be empty");
        String rawCost = System.getProperty(STEAK_COST, "64");
        if (!rawCost.matches("[1-9][0-9]{0,79}")) {
            throw new IllegalArgumentException("Infinite Steak EMC cost must be a canonical positive integer");
        }
        return new Snapshot(finalStar, Set.copyOf(slots), finalCooldown, consumables,
            new EmcValue(new BigInteger(rawCost)), steakCooldown);
    }

    private static boolean bool(String key, boolean fallback) {
        String value = System.getProperty(key);
        if (value == null) return fallback;
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        throw new IllegalArgumentException(key + " must be true or false");
    }
    private static int integer(String key, int fallback, int min, int max) {
        String value = System.getProperty(key);
        if (value == null) return fallback;
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < min || parsed > max) throw new IllegalArgumentException(key + " is outside safe bounds");
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(key + " must be an integer", exception);
        }
    }

    public record Snapshot(
        boolean finalStarEnabled, Set<FinalStarSlot> finalStarSlots, int finalStarCooldownTicks,
        boolean infiniteConsumablesEnabled, EmcValue infiniteSteakCost, int infiniteSteakCooldownTicks
    ) { }
}
