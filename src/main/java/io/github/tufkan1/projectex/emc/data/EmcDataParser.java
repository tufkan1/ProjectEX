package io.github.tufkan1.projectex.emc.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.io.Reader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/** Strict parser for ProjectEX EMC schema version 1. Registry validation happens later. */
public final class EmcDataParser {
    public static final int MAX_EMC_DIGITS = 1_000;

    private static final Set<String> ROOT_FIELDS = Set.of("schema_version", "priority", "values");
    private static final Set<String> ENTRY_FIELDS = Set.of("item", "components", "emc", "alias", "remove");
    private static final Gson GSON = new Gson();

    private EmcDataParser() {
    }

    public static EmcDataFile parse(Reader reader) {
        try {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) {
                throw new EmcDataException("EMC data root must be an object");
            }
            JsonObject root = parsed.getAsJsonObject();
            rejectUnknownFields(root, ROOT_FIELDS, "root");

            int schemaVersion = requiredInt(root, "schema_version");
            if (schemaVersion != EmcDataFile.CURRENT_SCHEMA_VERSION) {
                throw new EmcDataException("Unsupported schema_version: " + schemaVersion);
            }

            int priority = root.has("priority") ? requiredInt(root, "priority") : 0;
            if (priority < EmcDataFile.MIN_PRIORITY || priority > EmcDataFile.MAX_PRIORITY) {
                throw new EmcDataException("priority must be between " + EmcDataFile.MIN_PRIORITY
                    + " and " + EmcDataFile.MAX_PRIORITY);
            }

            JsonElement valuesElement = required(root, "values");
            if (!valuesElement.isJsonArray()) {
                throw new EmcDataException("values must be an array");
            }

            List<EmcDefinition> definitions = new ArrayList<>();
            Set<String> matchKeys = new HashSet<>();
            JsonArray values = valuesElement.getAsJsonArray();
            for (int index = 0; index < values.size(); index++) {
                EmcDefinition definition = parseDefinition(values.get(index), index);
                if (!matchKeys.add(definition.matchKey())) {
                    throw new EmcDataException("Duplicate item/components definition at values[" + index
                        + "]: " + definition.matchKey());
                }
                definitions.add(definition);
            }
            return new EmcDataFile(schemaVersion, priority, definitions);
        } catch (EmcDataException exception) {
            throw exception;
        } catch (JsonParseException | IllegalStateException | NumberFormatException exception) {
            throw new EmcDataException("Invalid EMC JSON: " + exception.getMessage(), exception);
        }
    }

    private static EmcDefinition parseDefinition(JsonElement element, int index) {
        if (!element.isJsonObject()) {
            throw new EmcDataException("values[" + index + "] must be an object");
        }
        JsonObject entry = element.getAsJsonObject();
        String location = "values[" + index + "]";
        rejectUnknownFields(entry, ENTRY_FIELDS, location);

        EmcKey item = parseKey(requiredString(entry, "item"), location + ".item");
        String components = null;
        if (entry.has("components")) {
            JsonElement componentElement = entry.get("components");
            if (!componentElement.isJsonObject()) {
                throw new EmcDataException(location + ".components must be an object");
            }
            components = GSON.toJson(canonicalize(componentElement));
        }

        int operations = (entry.has("emc") ? 1 : 0)
            + (entry.has("alias") ? 1 : 0)
            + (entry.has("remove") ? 1 : 0);
        if (operations != 1) {
            throw new EmcDataException(location + " must contain exactly one of emc, alias, or remove");
        }

        if (entry.has("emc")) {
            String amount = requiredString(entry, "emc");
            if (!amount.matches("0|[1-9][0-9]*")) {
                throw new EmcDataException(location + ".emc must be a canonical non-negative integer string");
            }
            if (amount.length() > MAX_EMC_DIGITS) {
                throw new EmcDataException(location + ".emc exceeds the " + MAX_EMC_DIGITS + " digit limit");
            }
            return new EmcDefinition(item, components, EmcDefinition.Kind.VALUE,
                new EmcValue(new BigInteger(amount)), null);
        }
        if (entry.has("alias")) {
            EmcKey alias = parseKey(requiredString(entry, "alias"), location + ".alias");
            return new EmcDefinition(item, components, EmcDefinition.Kind.ALIAS, null, alias);
        }

        JsonElement remove = entry.get("remove");
        if (!remove.isJsonPrimitive() || !remove.getAsJsonPrimitive().isBoolean() || !remove.getAsBoolean()) {
            throw new EmcDataException(location + ".remove must be true");
        }
        return new EmcDefinition(item, components, EmcDefinition.Kind.REMOVE, null, null);
    }

    private static JsonElement canonicalize(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject sorted = new JsonObject();
            TreeMap<String, JsonElement> fields = new TreeMap<>();
            element.getAsJsonObject().entrySet().forEach(entry -> fields.put(entry.getKey(), entry.getValue()));
            fields.forEach((key, value) -> sorted.add(key, canonicalize(value)));
            return sorted;
        }
        if (element.isJsonArray()) {
            JsonArray array = new JsonArray();
            element.getAsJsonArray().forEach(value -> array.add(canonicalize(value)));
            return array;
        }
        return element.deepCopy();
    }

    private static EmcKey parseKey(String value, String location) {
        try {
            return EmcKey.parse(value);
        } catch (IllegalArgumentException exception) {
            throw new EmcDataException(location + " is not a valid namespace:path identifier", exception);
        }
    }

    private static JsonElement required(JsonObject object, String field) {
        JsonElement value = object.get(field);
        if (value == null || value.isJsonNull()) {
            throw new EmcDataException("Missing required field: " + field);
        }
        return value;
    }

    private static String requiredString(JsonObject object, String field) {
        JsonElement value = required(object, field);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new EmcDataException(field + " must be a string");
        }
        return value.getAsString();
    }

    private static int requiredInt(JsonObject object, String field) {
        JsonElement value = required(object, field);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new EmcDataException(field + " must be an integer");
        }
        try {
            return value.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException exception) {
            throw new EmcDataException(field + " must be an integer", exception);
        }
    }

    private static void rejectUnknownFields(JsonObject object, Set<String> allowed, String location) {
        object.keySet().stream()
            .filter(field -> !allowed.contains(field))
            .findFirst()
            .ifPresent(field -> {
                throw new EmcDataException("Unknown field at " + location + ": " + field);
            });
    }
}
