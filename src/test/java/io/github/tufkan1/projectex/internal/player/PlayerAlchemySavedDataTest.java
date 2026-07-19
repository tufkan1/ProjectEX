package io.github.tufkan1.projectex.internal.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerAlchemySavedDataTest {
    @Test
    void preservesTheRawPayloadWhenDomainDataIsCorrupt() {
        JsonObject encoded = new JsonObject();
        encoded.addProperty("payload", "{not-json");

        PlayerAlchemySavedData recovered = PlayerAlchemySavedData.CODEC
            .parse(JsonOps.INSTANCE, encoded)
            .getOrThrow();

        assertTrue(recovered.snapshot().isEmpty());
        assertEquals("{not-json", recovered.recoveryPayload().orElseThrow());
        assertTrue(recovered.recoveryError().isPresent());

        var reencoded = PlayerAlchemySavedData.CODEC.encodeStart(JsonOps.INSTANCE, recovered).getOrThrow();
        PlayerAlchemySavedData reloaded = PlayerAlchemySavedData.CODEC
            .parse(JsonOps.INSTANCE, reencoded)
            .getOrThrow();
        assertEquals("{not-json", reloaded.recoveryPayload().orElseThrow());
    }

    @Test
    void savedDataCodecSurvivesAFullEncodeDecodeCycle() {
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000001");
        PlayerAlchemySavedData data = new PlayerAlchemySavedData();
        data.update(player, state -> state.credit(EmcValue.of(4096)));

        var encoded = PlayerAlchemySavedData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
        PlayerAlchemySavedData reloaded = PlayerAlchemySavedData.CODEC
            .parse(JsonOps.INSTANCE, encoded)
            .getOrThrow();

        assertEquals(EmcValue.of(4096), reloaded.state(player).balance());
    }
}
