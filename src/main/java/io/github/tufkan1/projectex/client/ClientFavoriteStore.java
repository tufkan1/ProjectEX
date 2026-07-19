package io.github.tufkan1.projectex.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.tufkan1.projectex.api.emc.EmcKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

/** Versioned, bounded client preference storage that tolerates missing or damaged files. */
public final class ClientFavoriteStore {
    public static final int FORMAT_VERSION = 1;
    private final Path path;

    public ClientFavoriteStore(Path path) {
        this.path = java.util.Objects.requireNonNull(path, "path");
    }

    public SortedSet<String> load() {
        if (!Files.isRegularFile(path)) {
            return immutable(new TreeSet<>());
        }
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (!root.has("version") || root.get("version").getAsInt() != FORMAT_VERSION
                || !root.has("favorites") || !root.get("favorites").isJsonArray()) {
                return immutable(new TreeSet<>());
            }
            TreeSet<String> result = new TreeSet<>();
            for (JsonElement element : root.getAsJsonArray("favorites")) {
                if (result.size() == ClientKnowledgeBrowserState.MAX_FAVORITES) {
                    break;
                }
                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                    addIfValid(result, element.getAsString());
                }
            }
            return immutable(result);
        } catch (IOException | RuntimeException exception) {
            return immutable(new TreeSet<>());
        }
    }

    public boolean save(SortedSet<String> favorites) {
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            JsonObject root = new JsonObject();
            root.addProperty("version", FORMAT_VERSION);
            JsonArray values = new JsonArray();
            favorites.stream().limit(ClientKnowledgeBrowserState.MAX_FAVORITES)
                .forEach(values::add);
            root.add("favorites", values);
            Files.writeString(temporary, root.toString(), StandardCharsets.UTF_8);
            moveIntoPlace(temporary);
            return true;
        } catch (IOException | RuntimeException exception) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException ignored) {
                // Best-effort cleanup; the canonical file was never replaced.
            }
            return false;
        }
    }

    private void moveIntoPlace(Path temporary) throws IOException {
        try {
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void addIfValid(SortedSet<String> target, String itemId) {
        try {
            EmcKey.parse(itemId);
            target.add(itemId);
        } catch (IllegalArgumentException ignored) {
            // Invalid or obsolete entries are skipped independently.
        }
    }

    private static SortedSet<String> immutable(SortedSet<String> values) {
        return Collections.unmodifiableSortedSet(new TreeSet<>(values));
    }
}
