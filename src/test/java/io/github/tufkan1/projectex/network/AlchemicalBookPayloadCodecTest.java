package io.github.tufkan1.projectex.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.tufkan1.projectex.teleport.AlchemicalBookLocations;
import io.github.tufkan1.projectex.teleport.AlchemicalDestination;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

final class AlchemicalBookPayloadCodecTest {
    @Test void actionAndBoundedViewRoundTrip() {
        UUID session = UUID.randomUUID();
        AlchemicalBookActionPayload action = new AlchemicalBookActionPayload(
            1, session, 7, AlchemicalBookAction.TELEPORT.ordinal(), "Home");
        AlchemicalDestination destination = new AlchemicalDestination(
            "Home", "minecraft:overworld", 1, 64, 2);
        AlchemicalBookViewPayload view = new AlchemicalBookViewPayload(
            session, 7, 2, true, "12345", "", List.of(
                new AlchemicalBookViewPayload.Entry(destination, "500")), Optional.empty());
        RegistryFriendlyByteBuf buffer = buffer();
        try {
            AlchemicalBookActionPayload.CODEC.encode(buffer, action);
            assertEquals(action, AlchemicalBookActionPayload.CODEC.decode(buffer));
            buffer.clear();
            AlchemicalBookViewPayload.CODEC.encode(buffer, view);
            assertEquals(view, AlchemicalBookViewPayload.CODEC.decode(buffer));
        } finally { buffer.release(); }
    }

    @Test void constructorsRejectOversizedNetworkFields() {
        assertThrows(IllegalArgumentException.class, () -> new AlchemicalBookActionPayload(
            1, UUID.randomUUID(), 0, 0, "x".repeat(AlchemicalDestination.MAX_NAME_LENGTH + 1)));
        assertThrows(IllegalArgumentException.class, () -> new AlchemicalBookViewPayload(
            UUID.randomUUID(), 0, 0, false, "0", "", java.util.Collections.nCopies(
                AlchemicalBookLocations.MAX_DESTINATIONS + 1,
                new AlchemicalBookViewPayload.Entry(new AlchemicalDestination(
                    "Home", "minecraft:overworld", 0, 64, 0), "0")), Optional.empty()));
    }

    private static RegistryFriendlyByteBuf buffer() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    }
}
