package io.github.tufkan1.projectex.player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.math.BigInteger;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

/** Strict, deterministic schema and v0-to-v1 migration for persisted player state. */
public final class PlayerAlchemyStateCodec {
    public static final int SCHEMA_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private PlayerAlchemyStateCodec() {
    }

    public static String encode(Map<UUID, PlayerAlchemyState> states) {
        JsonObject root = new JsonObject();
        root.addProperty("schema_version", SCHEMA_VERSION);
        JsonObject players = new JsonObject();
        new TreeMap<>(states).forEach((uuid, state) -> players.add(uuid.toString(), encodeState(state)));
        root.add("players", players);
        return GSON.toJson(root);
    }

    public static SortedMap<UUID, PlayerAlchemyState> decode(String json) {
        try {
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            if (root == null) {
                throw new PlayerStateDataException("Player state root must be an object");
            }
            int version = requiredInt(root, "schema_version");
            if (version == 0) {
                return decodeVersionZero(root);
            }
            if (version != SCHEMA_VERSION) {
                throw new PlayerStateDataException("Unsupported player state schema version: " + version);
            }
            requireOnly(root, "schema_version", "players");
            return decodePlayers(requiredObject(root, "players"), version);
        } catch (JsonParseException | ArithmeticException | IllegalArgumentException exception) {
            throw new PlayerStateDataException("Invalid player state JSON", exception);
        }
    }

    private static SortedMap<UUID, PlayerAlchemyState> decodeVersionZero(JsonObject root) {
        requireOnly(root, "schema_version", "players");
        return decodePlayers(requiredObject(root, "players"), 0);
    }

    private static SortedMap<UUID, PlayerAlchemyState> decodePlayers(JsonObject players, int version) {
        SortedMap<UUID, PlayerAlchemyState> result = new TreeMap<>();
        players.entrySet().forEach(entry -> {
            UUID uuid;
            try {
                uuid = UUID.fromString(entry.getKey());
            } catch (IllegalArgumentException exception) {
                throw new PlayerStateDataException("Invalid player UUID: " + entry.getKey(), exception);
            }
            if (!entry.getValue().isJsonObject()) {
                throw new PlayerStateDataException("State for " + uuid + " must be an object");
            }
            result.put(uuid, decodeState(entry.getValue().getAsJsonObject(), uuid, version));
        });
        return result;
    }

    private static JsonObject encodeState(PlayerAlchemyState state) {
        JsonObject encoded = new JsonObject();
        encoded.addProperty("balance", state.balance().amount().toString());
        JsonArray knowledge = new JsonArray();
        state.knowledge().forEach(item -> knowledge.add(item.toString()));
        encoded.add("knowledge", knowledge);
        return encoded;
    }

    private static PlayerAlchemyState decodeState(JsonObject encoded, UUID uuid, int version) {
        requireOnly(encoded, version == 0 ? new String[] {"emc", "knowledge"} : new String[] {"balance", "knowledge"});
        String balanceText = requiredString(encoded, version == 0 ? "emc" : "balance");
        if (balanceText.length() > PlayerAlchemyState.MAX_BALANCE_DIGITS) {
            throw new PlayerStateDataException("EMC balance exceeds safe digit limit for " + uuid);
        }
        BigInteger balance = new BigInteger(balanceText);
        if (balance.signum() < 0) {
            throw new PlayerStateDataException("Negative EMC balance for " + uuid);
        }
        JsonArray knowledgeJson = requiredArray(encoded, "knowledge");
        if (knowledgeJson.size() > PlayerAlchemyState.MAX_KNOWLEDGE_ENTRIES) {
            throw new PlayerStateDataException("Knowledge exceeds safe entry limit for " + uuid);
        }
        TreeSet<EmcKey> knowledge = new TreeSet<>();
        for (JsonElement element : knowledgeJson) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                throw new PlayerStateDataException("Knowledge entries for " + uuid + " must be strings");
            }
            knowledge.add(EmcKey.parse(element.getAsString()));
        }
        return new PlayerAlchemyState(new EmcValue(balance), knowledge);
    }

    private static JsonObject requiredObject(JsonObject object, String name) {
        JsonElement value = object.get(name);
        if (value == null || !value.isJsonObject()) {
            throw new PlayerStateDataException("Required object missing: " + name);
        }
        return value.getAsJsonObject();
    }

    private static JsonArray requiredArray(JsonObject object, String name) {
        JsonElement value = object.get(name);
        if (value == null || !value.isJsonArray()) {
            throw new PlayerStateDataException("Required array missing: " + name);
        }
        return value.getAsJsonArray();
    }

    private static String requiredString(JsonObject object, String name) {
        JsonElement value = object.get(name);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new PlayerStateDataException("Required string missing: " + name);
        }
        return value.getAsString();
    }

    private static int requiredInt(JsonObject object, String name) {
        JsonElement value = object.get(name);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new PlayerStateDataException("Required integer missing: " + name);
        }
        return value.getAsBigDecimal().intValueExact();
    }

    private static void requireOnly(JsonObject object, String... allowed) {
        java.util.Set<String> names = java.util.Set.of(allowed);
        object.keySet().forEach(name -> {
            if (!names.contains(name)) {
                throw new PlayerStateDataException("Unknown player state field: " + name);
            }
        });
    }
}
