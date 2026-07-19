package io.github.tufkan1.projectex.internal.teleport;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.teleport.AlchemicalBookLocations;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.UnaryOperator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** UUID-keyed destinations used while an Alchemical Book is player-bound. */
public final class AlchemicalBookSavedData extends SavedData {
    static final Codec<AlchemicalBookSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.unboundedMap(Codec.STRING, AlchemicalBookLocations.CODEC).fieldOf("players")
            .forGetter(AlchemicalBookSavedData::encoded)
    ).apply(instance, AlchemicalBookSavedData::decode));
    private static final SavedDataType<AlchemicalBookSavedData> TYPE = new SavedDataType<>(
        ProjectEX.id("alchemical_book_locations"), AlchemicalBookSavedData::new, CODEC,
        DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final TreeMap<UUID, AlchemicalBookLocations> players;
    private final java.util.HashMap<UUID, Long> revisions = new java.util.HashMap<>();

    public AlchemicalBookSavedData() { this(Map.of()); }
    private AlchemicalBookSavedData(Map<UUID, AlchemicalBookLocations> players) {
        this.players = new TreeMap<>(players);
    }

    public static AlchemicalBookSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public synchronized AlchemicalBookLocations locations(UUID owner) {
        return players.getOrDefault(owner, AlchemicalBookLocations.EMPTY);
    }

    public synchronized long revision(UUID owner) { return revisions.getOrDefault(owner, 0L); }

    public synchronized boolean compareAndSet(
        UUID owner, AlchemicalBookLocations expected, long expectedRevision, AlchemicalBookLocations replacement
    ) {
        if (revision(owner) != expectedRevision || !locations(owner).equals(expected)) return false;
        if (replacement.equals(AlchemicalBookLocations.EMPTY)) players.remove(owner);
        else players.put(owner, replacement);
        if (!replacement.equals(expected)) {
            revisions.merge(owner, 1L, (current, increment) -> current == Long.MAX_VALUE ? current : current + increment);
            setDirty();
        }
        return true;
    }

    public synchronized AlchemicalBookLocations update(UUID owner, UnaryOperator<AlchemicalBookLocations> update) {
        AlchemicalBookLocations before = locations(owner);
        AlchemicalBookLocations after = java.util.Objects.requireNonNull(update.apply(before));
        if (!compareAndSet(owner, before, revision(owner), after)) throw new IllegalStateException("Concurrent update");
        return after;
    }

    public synchronized Map<UUID, AlchemicalBookLocations> snapshot() { return Map.copyOf(players); }

    private synchronized Map<String, AlchemicalBookLocations> encoded() {
        TreeMap<String, AlchemicalBookLocations> result = new TreeMap<>();
        players.forEach((id, locations) -> result.put(id.toString(), locations));
        return result;
    }

    private static AlchemicalBookSavedData decode(Map<String, AlchemicalBookLocations> encoded) {
        TreeMap<UUID, AlchemicalBookLocations> decoded = new TreeMap<>();
        encoded.forEach((id, locations) -> decoded.put(UUID.fromString(id), locations));
        return new AlchemicalBookSavedData(decoded);
    }
}
