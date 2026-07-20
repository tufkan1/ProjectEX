package io.github.tufkan1.projectex.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

final class BlockAssetIntegrityTest {
    private static final Path GENERATED = Path.of("src/main/generated/assets");
    private static final Path RESOURCES = Path.of("src/main/resources/assets");

    @Test
    void everyGeneratedBlockModelUsesAResolvableBlockParentAndTextures() throws IOException {
        Path models = GENERATED.resolve("projectex/models/block");
        try (Stream<Path> files = Files.list(models)) {
            for (Path path : files.filter(file -> file.toString().endsWith(".json")).toList()) {
                JsonObject model = read(path);
                if (model.has("parent")) {
                    String parent = model.get("parent").getAsString();
                    assertFalse(parent.contains(":item/"),
                        () -> path + " uses an item model as a placed-block parent: " + parent);
                    assertTrue(modelExists(parent), () -> path + " has missing parent " + parent);
                }
                if (model.has("textures")) {
                    for (JsonElement texture : model.getAsJsonObject("textures").asMap().values()) {
                        String id = texture.getAsString();
                        if (!id.startsWith("#")) {
                            assertTrue(textureExists(id), () -> path + " has missing texture " + id);
                        }
                    }
                }
            }
        }
    }

    @Test
    void everyGeneratedBlockstateReferencesARealModel() throws IOException {
        Path states = GENERATED.resolve("projectex/blockstates");
        try (Stream<Path> files = Files.list(states)) {
            for (Path path : files.filter(file -> file.toString().endsWith(".json")).toList()) {
                JsonObject variants = read(path).getAsJsonObject("variants");
                assertTrue(variants != null, () -> path + " has no variants object");
                for (JsonElement variant : variants.asMap().values()) {
                    JsonObject definition = variant.getAsJsonObject();
                    String model = definition.get("model").getAsString();
                    assertTrue(modelExists(model), () -> path + " has missing model " + model);
                    if (definition.has("y")) {
                        int rotation = definition.get("y").getAsInt();
                        assertTrue(rotation == 0 || rotation == 90 || rotation == 180 || rotation == 270,
                            () -> path + " has unsafe Y rotation " + rotation);
                    }
                }
            }
        }
    }

    private static boolean modelExists(String id) {
        String[] split = split(id);
        if (split[0].equals("minecraft")) return true;
        String relative = split[0] + "/models/" + split[1] + ".json";
        return Files.isRegularFile(GENERATED.resolve(relative))
            || Files.isRegularFile(RESOURCES.resolve(relative));
    }

    private static boolean textureExists(String id) {
        String[] split = split(id);
        if (split[0].equals("minecraft")) return true;
        String relative = split[0] + "/textures/" + split[1] + ".png";
        return Files.isRegularFile(GENERATED.resolve(relative))
            || Files.isRegularFile(RESOURCES.resolve(relative));
    }

    private static String[] split(String id) {
        int separator = id.indexOf(':');
        return separator < 0 ? new String[] {"minecraft", id}
            : new String[] {id.substring(0, separator), id.substring(separator + 1)};
    }

    private static JsonObject read(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
