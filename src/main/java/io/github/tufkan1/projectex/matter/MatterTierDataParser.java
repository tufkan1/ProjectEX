package io.github.tufkan1.projectex.matter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.io.Reader;
import java.math.BigInteger;
import java.util.Set;

/** Strict schema-v1 parser for one complete data-pack matter tier definition. */
public final class MatterTierDataParser {
    private static final Set<String> FIELDS = Set.of(
        "schema_version", "id", "mining_level", "mining_speed", "attack_bonus",
        "max_charge", "max_area_blocks", "action_cooldown_ticks", "emc_per_area_block",
        "furnace_cook_ticks", "furnace_output_slots", "bonus_output_numerator",
        "bonus_output_denominator", "armor_damage_reduction_cap"
    );

    private MatterTierDataParser() { }

    public static MatterTier parse(Reader reader) {
        try {
            JsonElement rootElement = JsonParser.parseReader(reader);
            if (!rootElement.isJsonObject()) throw new IllegalArgumentException("Tier root must be an object");
            JsonObject root = rootElement.getAsJsonObject();
            root.keySet().stream().filter(field -> !FIELDS.contains(field)).findFirst()
                .ifPresent(field -> { throw new IllegalArgumentException("Unknown tier field: " + field); });
            if (integer(root, "schema_version") != 1) {
                throw new IllegalArgumentException("Unsupported matter tier schema_version");
            }
            String emc = string(root, "emc_per_area_block");
            if (!emc.matches("0|[1-9][0-9]{0,39}")) {
                throw new IllegalArgumentException("emc_per_area_block must be a bounded canonical integer string");
            }
            return new MatterTier(
                string(root, "id"), integer(root, "mining_level"), decimal(root, "mining_speed"),
                decimal(root, "attack_bonus"), integer(root, "max_charge"),
                integer(root, "max_area_blocks"), integer(root, "action_cooldown_ticks"),
                new EmcValue(new BigInteger(emc)), integer(root, "furnace_cook_ticks"),
                integer(root, "furnace_output_slots"), integer(root, "bonus_output_numerator"),
                integer(root, "bonus_output_denominator"), decimal(root, "armor_damage_reduction_cap")
            );
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid matter tier JSON: " + exception.getMessage(), exception);
        }
    }

    private static JsonElement required(JsonObject root, String field) {
        JsonElement value = root.get(field);
        if (value == null || value.isJsonNull()) throw new IllegalArgumentException("Missing tier field: " + field);
        return value;
    }
    private static String string(JsonObject root, String field) {
        JsonElement value = required(root, field);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException(field + " must be a string");
        }
        return value.getAsString();
    }
    private static int integer(JsonObject root, String field) {
        try {
            JsonElement value = required(root, field);
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
                throw new NumberFormatException("not a number");
            }
            return value.getAsBigDecimal().intValueExact();
        }
        catch (RuntimeException exception) { throw new IllegalArgumentException(field + " must be an integer", exception); }
    }
    private static double decimal(JsonObject root, String field) {
        try {
            JsonElement element = required(root, field);
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
                throw new NumberFormatException("not a number");
            }
            double value = element.getAsDouble();
            if (!Double.isFinite(value)) throw new NumberFormatException("non-finite");
            return value;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(field + " must be a finite number", exception);
        }
    }
}
