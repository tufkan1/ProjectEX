package io.github.tufkan1.projectex.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mojang.serialization.JsonOps;
import io.github.tufkan1.projectex.content.component.ActiveItemMode;
import io.github.tufkan1.projectex.content.component.ActiveItemState;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

class ActiveItemStateTest {
    @Test
    void persistentCodecRoundTripsCurrentVersion() {
        ActiveItemState original = new ActiveItemState(1, 2, ActiveItemMode.LINE);
        var json = ActiveItemState.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();

        assertEquals(original, ActiveItemState.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void networkCodecRoundTripsAllFields() {
        ActiveItemState original = new ActiveItemState(1, 1, ActiveItemMode.PANEL);
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
            Unpooled.buffer(),
            RegistryAccess.EMPTY
        );
        try {
            ActiveItemState.STREAM_CODEC.encode(buffer, original);
            assertEquals(original, ActiveItemState.STREAM_CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void chargeAndModeCyclingAreBounded() {
        assertEquals(0, ActiveItemState.DEFAULT.nextCharge().nextCharge().nextCharge().charge());
        assertEquals(
            ActiveItemMode.CUBE,
            ActiveItemState.DEFAULT.nextMode().nextMode().nextMode().mode()
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new ActiveItemState(1, ActiveItemState.MAX_CHARGE + 1, ActiveItemMode.CUBE)
        );
    }
}
