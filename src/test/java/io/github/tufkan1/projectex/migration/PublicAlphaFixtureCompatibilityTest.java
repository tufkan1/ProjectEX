package io.github.tufkan1.projectex.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.client.ClientFavoriteStore;
import io.github.tufkan1.projectex.content.component.AlchemyStorageState;
import io.github.tufkan1.projectex.content.component.MachineItemState;
import io.github.tufkan1.projectex.player.PlayerAlchemyStateCodec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PublicAlphaFixtureCompatibilityTest {
    private static final String ROOT = "/fixtures/0.1.0-alpha.1/";
    @TempDir Path directory;

    @Test
    void upgradesPlayerBlockItemAndPreferenceFixturesWithoutLoss() throws Exception {
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var players = PlayerAlchemyStateCodec.decode(resource("player-v0.json"));
        assertEquals(EmcValue.of(4096), players.get(player).balance());
        assertEquals(2, players.get(player).knowledge().size());
        assertTrue(PlayerAlchemyStateCodec.encode(players).contains("\"schema_version\":1"));

        MachineItemState machine = MachineItemState.CODEC.parse(JsonOps.INSTANCE,
            JsonParser.parseString(resource("machine-item-v1.json"))).getOrThrow();
        assertEquals(EmcValue.of(8192), machine.stored());
        assertEquals(player, machine.owner().orElseThrow());

        AlchemyStorageState storage = AlchemyStorageState.CODEC.parse(JsonOps.INSTANCE,
            JsonParser.parseString(resource("storage-block-v1.json"))).getOrThrow();
        assertEquals(EmcValue.of(16384), storage.stored());
        assertEquals(2, storage.advancedConfig().itemIds().size());

        Path preferences = directory.resolve("projectex-favorites.json");
        Files.writeString(preferences, resource("client-preferences-v1.json"));
        assertEquals(2, new ClientFavoriteStore(preferences).load().size());
    }

    private static String resource(String name) throws Exception {
        try (var input = PublicAlphaFixtureCompatibilityTest.class.getResourceAsStream(ROOT + name)) {
            if (input == null) throw new IllegalStateException("Missing fixture " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
