package io.github.tufkan1.projectex.machine;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Strict string-map codec used as the block-entity persistence migration boundary. */
public final class MachineStateCodec {
    private static final int MAX_DECIMAL_DIGITS = 4096;

    private MachineStateCodec() {
    }

    public static Map<String, String> encode(MachineState state) {
        Map<String, String> encoded = new LinkedHashMap<>();
        encoded.put("version", Integer.toString(state.version()));
        encoded.put("tier", state.tier().name());
        encoded.put("stored", state.stored().amount().toString());
        encoded.put("deferred_generation", state.deferredGeneration().toString());
        encoded.put("owner", state.access().owner().map(UUID::toString).orElse(""));
        encoded.put("public_access", Boolean.toString(state.access().publicAccess()));
        encoded.put("redstone_mode", state.redstoneMode().name());
        return Map.copyOf(encoded);
    }

    public static MachineState decode(Map<String, String> encoded, MachineTier expectedTier) {
        try {
            int version = Integer.parseInt(require(encoded, "version"));
            MachineTier tier = MachineTier.valueOf(require(encoded, "tier"));
            if (tier != expectedTier) {
                throw new IllegalArgumentException("Machine tier does not match its block type");
            }
            EmcValue stored = new EmcValue(decimal(require(encoded, "stored"), "stored"));
            BigInteger deferred = decimal(
                encoded.getOrDefault("deferred_generation", "0"),
                "deferred_generation"
            );
            String ownerText = encoded.getOrDefault("owner", "");
            Optional<UUID> owner = ownerText.isBlank()
                ? Optional.empty()
                : Optional.of(UUID.fromString(ownerText));
            boolean publicAccess = strictBoolean(encoded.getOrDefault("public_access", "false"));
            MachineRedstoneMode mode = MachineRedstoneMode.valueOf(
                encoded.getOrDefault("redstone_mode", MachineRedstoneMode.IGNORED.name())
            );
            return new MachineState(
                version,
                tier,
                stored,
                deferred,
                new MachineAccess(owner, publicAccess),
                mode
            );
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Malformed machine state", exception);
        }
    }

    private static String require(Map<String, String> encoded, String key) {
        String value = encoded.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing machine state field: " + key);
        }
        return value;
    }

    private static BigInteger decimal(String value, String field) {
        if (value.isEmpty() || value.length() > MAX_DECIMAL_DIGITS
            || !value.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("Invalid decimal machine field: " + field);
        }
        return new BigInteger(value);
    }

    private static boolean strictBoolean(String value) {
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new IllegalArgumentException("Invalid boolean machine field");
        }
        return Boolean.parseBoolean(value);
    }
}
