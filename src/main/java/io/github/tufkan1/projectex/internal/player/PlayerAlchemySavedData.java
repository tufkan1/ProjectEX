package io.github.tufkan1.projectex.internal.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.player.PlayerAlchemyState;
import io.github.tufkan1.projectex.player.PlayerAlchemyStateCodec;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.UnaryOperator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Server-owned UUID-keyed persistence. UUID identity naturally survives clone and dimension changes. */
public final class PlayerAlchemySavedData extends SavedData {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerAlchemySavedData.class);
    private static final int NBT_STRING_CHUNK_CHARACTERS = 16_000;
    private static final int MAX_RECOVERY_ERROR_CHARACTERS = 4_096;
    private static final Codec<List<String>> CHUNKED_STRING_CODEC = Codec.STRING.listOf()
        .withAlternative(Codec.STRING, List::of);
    static final Codec<PlayerAlchemySavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        CHUNKED_STRING_CODEC.fieldOf("payload").forGetter(data -> chunks(data.payload())),
        CHUNKED_STRING_CODEC.optionalFieldOf("recovery_payload")
            .forGetter(data -> data.recoveryPayload.map(PlayerAlchemySavedData::chunks)),
        Codec.STRING.optionalFieldOf("recovery_error")
            .forGetter(data -> data.recoveryError.map(PlayerAlchemySavedData::boundedError))
    ).apply(instance, PlayerAlchemySavedData::fromChunks));

    private static final SavedDataType<PlayerAlchemySavedData> TYPE = new SavedDataType<>(
        ProjectEX.id("player_alchemy"),
        PlayerAlchemySavedData::new,
        CODEC,
        DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private final Map<UUID, PlayerAlchemyState> states;
    private final Map<UUID, Long> revisions = new java.util.HashMap<>();
    private Optional<String> recoveryPayload;
    private Optional<String> recoveryError;

    public PlayerAlchemySavedData() {
        this(Map.of(), Optional.empty(), Optional.empty());
    }

    private PlayerAlchemySavedData(
        Map<UUID, PlayerAlchemyState> states,
        Optional<String> recoveryPayload,
        Optional<String> recoveryError
    ) {
        this.states = new TreeMap<>(states);
        this.recoveryPayload = recoveryPayload;
        this.recoveryError = recoveryError;
    }

    public static PlayerAlchemySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public synchronized PlayerAlchemyState state(UUID playerId) {
        return states.getOrDefault(playerId, PlayerAlchemyState.EMPTY);
    }

    /** Runtime monotonic revision used by transactional automation and storage cache invalidation. */
    public synchronized long revision(UUID playerId) {
        return revisions.getOrDefault(playerId, 0L);
    }

    public synchronized PlayerAlchemyState update(UUID playerId, UnaryOperator<PlayerAlchemyState> update) {
        PlayerAlchemyState before = state(playerId);
        PlayerAlchemyState after = java.util.Objects.requireNonNull(update.apply(before), "updated state");
        if (!after.equals(before)) {
            if (after.equals(PlayerAlchemyState.EMPTY)) {
                states.remove(playerId);
            } else {
                states.put(playerId, after);
            }
            bumpRevision(playerId);
            setDirty();
        }
        return after;
    }

    public synchronized Optional<PlayerAlchemyState> remove(UUID playerId) {
        Optional<PlayerAlchemyState> removed = Optional.ofNullable(states.remove(playerId));
        removed.ifPresent(ignored -> {
            bumpRevision(playerId);
            setDirty();
        });
        return removed;
    }

    public synchronized boolean compareAndSet(
        UUID playerId,
        PlayerAlchemyState expected,
        PlayerAlchemyState replacement
    ) {
        if (!state(playerId).equals(expected)) {
            return false;
        }
        if (replacement.equals(PlayerAlchemyState.EMPTY)) {
            states.remove(playerId);
        } else {
            states.put(playerId, replacement);
        }
        if (!replacement.equals(expected)) {
            bumpRevision(playerId);
            setDirty();
        }
        return true;
    }

    public synchronized boolean compareAndSet(
        UUID playerId,
        PlayerAlchemyState expected,
        long expectedRevision,
        PlayerAlchemyState replacement
    ) {
        if (revision(playerId) != expectedRevision) return false;
        return compareAndSet(playerId, expected, replacement);
    }

    public synchronized Map<UUID, PlayerAlchemyState> snapshot() {
        return Collections.unmodifiableMap(new TreeMap<>(states));
    }

    public synchronized Optional<String> recoveryPayload() {
        return recoveryPayload;
    }

    public synchronized Optional<String> recoveryError() {
        return recoveryError;
    }

    public synchronized void clearRecoveryBackup() {
        if (recoveryPayload.isPresent() || recoveryError.isPresent()) {
            recoveryPayload = Optional.empty();
            recoveryError = Optional.empty();
            setDirty();
        }
    }

    private synchronized String payload() {
        return PlayerAlchemyStateCodec.encode(states);
    }

    private void bumpRevision(UUID playerId) {
        revisions.merge(playerId, 1L, (current, increment) ->
            current == Long.MAX_VALUE ? Long.MAX_VALUE : current + increment);
    }

    private static PlayerAlchemySavedData fromPayload(
        String payload,
        Optional<String> previousRecoveryPayload,
        Optional<String> previousRecoveryError
    ) {
        try {
            return new PlayerAlchemySavedData(
                PlayerAlchemyStateCodec.decode(payload),
                previousRecoveryPayload,
                previousRecoveryError
            );
        } catch (RuntimeException exception) {
            LOGGER.error("Player alchemy data is corrupt; preserving the raw payload and starting recovery mode", exception);
            return new PlayerAlchemySavedData(
                Map.of(),
                Optional.of(payload),
                Optional.of(boundedError(
                    exception.getMessage() == null ? exception.getClass().getName() : exception.getMessage()))
            );
        }
    }

    private static PlayerAlchemySavedData fromChunks(
        List<String> payload,
        Optional<List<String>> previousRecoveryPayload,
        Optional<String> previousRecoveryError
    ) {
        return fromPayload(
            String.join("", payload),
            previousRecoveryPayload.map(chunks -> String.join("", chunks)),
            previousRecoveryError.map(PlayerAlchemySavedData::boundedError)
        );
    }

    private static List<String> chunks(String value) {
        if (value.isEmpty()) return List.of("");
        java.util.ArrayList<String> chunks = new java.util.ArrayList<>(
            (value.length() + NBT_STRING_CHUNK_CHARACTERS - 1) / NBT_STRING_CHUNK_CHARACTERS);
        for (int start = 0; start < value.length(); start += NBT_STRING_CHUNK_CHARACTERS) {
            chunks.add(value.substring(start, Math.min(value.length(), start + NBT_STRING_CHUNK_CHARACTERS)));
        }
        return List.copyOf(chunks);
    }

    private static String boundedError(String error) {
        return error.length() <= MAX_RECOVERY_ERROR_CHARACTERS
            ? error
            : error.substring(0, MAX_RECOVERY_ERROR_CHARACTERS);
    }
}
