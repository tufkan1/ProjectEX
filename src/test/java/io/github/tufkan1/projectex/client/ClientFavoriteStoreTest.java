package io.github.tufkan1.projectex.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClientFavoriteStoreTest {
    @TempDir
    Path directory;

    @Test
    void roundTripsSortedFavoritesAndReplacesExistingFile() {
        ClientFavoriteStore store = new ClientFavoriteStore(directory.resolve("favorites.json"));
        assertTrue(store.save(new TreeSet<>(java.util.List.of(
            "minecraft:diamond", "minecraft:coal"))));
        assertEquals(new TreeSet<>(java.util.List.of(
            "minecraft:coal", "minecraft:diamond")), store.load());

        assertTrue(store.save(new TreeSet<>(java.util.List.of("minecraft:emerald"))));
        assertEquals(new TreeSet<>(java.util.List.of("minecraft:emerald")), store.load());
    }

    @Test
    void damagedUnknownAndInvalidPreferencesFailClosed() throws Exception {
        Path path = directory.resolve("favorites.json");
        ClientFavoriteStore store = new ClientFavoriteStore(path);
        Files.writeString(path, "not-json");
        assertTrue(store.load().isEmpty());

        Files.writeString(path, "{\"version\":2,\"favorites\":[\"minecraft:coal\"]}");
        assertTrue(store.load().isEmpty());

        Files.writeString(path,
            "{\"version\":1,\"favorites\":[\"invalid\",\"minecraft:coal\",4]}");
        assertEquals(new TreeSet<>(java.util.List.of("minecraft:coal")), store.load());
    }
}
