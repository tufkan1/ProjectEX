package io.github.tufkan1.projectex.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.io.Reader;
import java.math.BigInteger;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/** Strict schema-v1 parser for the complete portable-star balance table. */
public final class KleinStarTierDataParser {
    private static final Set<String> ROOT_FIELDS = Set.of("schema_version", "tiers");
    private static final Set<String> TIER_FIELDS = Set.of("id", "capacity");

    private KleinStarTierDataParser() { }

    public static Map<KleinStarTier, EmcValue> parse(Reader reader) {
        try {
            JsonElement element = JsonParser.parseReader(reader);
            if (!element.isJsonObject()) throw new IllegalArgumentException("Star tier root must be an object");
            JsonObject root = element.getAsJsonObject();
            rejectUnknown(root, ROOT_FIELDS, "root");
            if (integer(root, "schema_version") != 1) {
                throw new IllegalArgumentException("Unsupported star tier schema_version");
            }
            JsonElement tiersElement = required(root, "tiers");
            if (!tiersElement.isJsonArray()) throw new IllegalArgumentException("tiers must be an array");
            EnumMap<KleinStarTier, EmcValue> result = new EnumMap<>(KleinStarTier.class);
            tiersElement.getAsJsonArray().forEach(tierElement -> {
                if (!tierElement.isJsonObject()) throw new IllegalArgumentException("Tier entry must be an object");
                JsonObject tierObject = tierElement.getAsJsonObject();
                rejectUnknown(tierObject, TIER_FIELDS, "tier");
                String id = string(tierObject, "id");
                KleinStarTier tier = java.util.Arrays.stream(KleinStarTier.values())
                    .filter(candidate -> candidate.serializedName().equals(id)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown star tier id: " + id));
                String capacity = string(tierObject, "capacity");
                if (!capacity.matches("[1-9][0-9]{0,79}")) {
                    throw new IllegalArgumentException("capacity must be a bounded canonical positive integer string");
                }
                if (result.put(tier, new EmcValue(new BigInteger(capacity))) != null) {
                    throw new IllegalArgumentException("Duplicate star tier id: " + id);
                }
            });
            if (result.size() != KleinStarTier.values().length) {
                throw new IllegalArgumentException("Star tier table must define every registered tier");
            }
            EmcValue previous = EmcValue.ZERO;
            for (KleinStarTier tier : KleinStarTier.values()) {
                EmcValue capacity = result.get(tier);
                if (capacity.compareTo(previous) <= 0) {
                    throw new IllegalArgumentException("Star capacities must be strictly increasing");
                }
                previous = capacity;
            }
            return Map.copyOf(result);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid star tier JSON: " + exception.getMessage(), exception);
        }
    }

    private static void rejectUnknown(JsonObject object, Set<String> fields, String context) {
        object.keySet().stream().filter(field -> !fields.contains(field)).findFirst()
            .ifPresent(field -> { throw new IllegalArgumentException("Unknown " + context + " field: " + field); });
    }
    private static JsonElement required(JsonObject object, String field) {
        JsonElement value = object.get(field);
        if (value == null || value.isJsonNull()) throw new IllegalArgumentException("Missing field: " + field);
        return value;
    }
    private static String string(JsonObject object, String field) {
        JsonElement value = required(object, field);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException(field + " must be a string");
        }
        return value.getAsString();
    }
    private static int integer(JsonObject object, String field) {
        try {
            JsonElement value = required(object, field);
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new NumberFormatException();
            return value.getAsBigDecimal().intValueExact();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(field + " must be an integer", exception);
        }
    }
}
