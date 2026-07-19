package io.github.tufkan1.projectex.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mojang.serialization.JsonOps;
import io.github.tufkan1.projectex.content.component.MatterToolState;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

class MatterToolStateTest {
    @Test
    void persistentAndNetworkCodecsRoundTrip() {
        MatterToolState original = new MatterToolState(1, 5);
        var json = MatterToolState.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        assertEquals(original, MatterToolState.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());

        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
            Unpooled.buffer(), RegistryAccess.EMPTY
        );
        try {
            MatterToolState.STREAM_CODEC.encode(buffer, original);
            assertEquals(original, MatterToolState.STREAM_CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void chargeCyclingIsTierBounded() {
        MatterToolState state = MatterToolState.DEFAULT;
        for (int i = 0; i < 5; i++) state = state.next(4);
        assertEquals(MatterToolState.DEFAULT, state);
        assertThrows(IllegalArgumentException.class, () -> MatterToolState.DEFAULT.next(17));
        assertThrows(IllegalArgumentException.class, () -> new MatterToolState(1, 17));
    }
}
