package io.github.tufkan1.projectex.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.content.component.PortableEmcState;
import io.netty.buffer.Unpooled;
import java.math.BigInteger;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

class PortableEmcStateTest {
    @Test
    void currentAndLegacyPersistentFormatsDecodeExactly() {
        PortableEmcState state = state("123456789012345678901234567890");
        var encoded = PortableEmcState.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow();

        assertEquals(state, PortableEmcState.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
        assertEquals(
            state,
            PortableEmcState.CODEC.parse(
                JsonOps.INSTANCE,
                JsonParser.parseString("{\"emc\":\"123456789012345678901234567890\"}")
            ).getOrThrow()
        );
    }

    @Test
    void networkFormatPreservesArbitraryPrecision() {
        PortableEmcState state = state("999999999999999999999999999999999999");
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
            Unpooled.buffer(),
            RegistryAccess.EMPTY
        );
        try {
            PortableEmcState.STREAM_CODEC.encode(buffer, state);
            assertEquals(state, PortableEmcState.STREAM_CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void malformedNegativeAndOversizedValuesAreRejected() {
        assertTrue(PortableEmcState.CODEC.parse(
            JsonOps.INSTANCE,
            JsonParser.parseString("{\"version\":1,\"stored\":\"-1\"}")
        ).error().isPresent());
        String oversized = "9".repeat(PortableEmcState.MAX_DIGITS + 1);
        assertTrue(PortableEmcState.CODEC.parse(
            JsonOps.INSTANCE,
            JsonParser.parseString("{\"version\":1,\"stored\":\"" + oversized + "\"}")
        ).error().isPresent());
    }

    private static PortableEmcState state(String amount) {
        return new PortableEmcState(
            PortableEmcState.CURRENT_VERSION,
            new EmcValue(new BigInteger(amount))
        );
    }
}
