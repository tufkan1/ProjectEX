package io.github.tufkan1.projectex.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tufkan1.projectex.alchemy.AlchemyTransactionFailure;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

class AlchemyPayloadCodecTest {
    @Test
    void actionPayloadRoundTripsEveryBoundedField() {
        AlchemyActionPayload original = new AlchemyActionPayload(1, 2, 3, 1, "minecraft:coal", 64, 7);
        RegistryFriendlyByteBuf buffer = buffer();
        try {
            AlchemyActionPayload.CODEC.encode(buffer, original);
            assertEquals(original, AlchemyActionPayload.CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void actionCodecRoundTripsAdversarialNumericValuesWithoutChangingTheirMeaning() {
        java.util.Random random = new java.util.Random(42);
        for (int index = 0; index < 1_000; index++) {
            AlchemyActionPayload original = new AlchemyActionPayload(
                random.nextInt(), random.nextLong(), random.nextLong(), random.nextInt(),
                "minecraft:coal", random.nextInt(), random.nextLong());
            RegistryFriendlyByteBuf buffer = buffer();
            try {
                AlchemyActionPayload.CODEC.encode(buffer, original);
                assertEquals(original, AlchemyActionPayload.CODEC.decode(buffer));
            } finally {
                buffer.release();
            }
        }
    }

    @Test
    void resultPayloadRoundTripsAndValidatesTypedFields() {
        AlchemyResultPayload original = new AlchemyResultPayload(
            1, 2, 3, false, AlchemyTransactionFailure.RATE_LIMITED.ordinal(), 7, "8192", 4);
        RegistryFriendlyByteBuf buffer = buffer();
        try {
            AlchemyResultPayload.CODEC.encode(buffer, original);
            AlchemyResultPayload decoded = AlchemyResultPayload.CODEC.decode(buffer);
            assertEquals(original, decoded);
            assertEquals(AlchemyTransactionFailure.RATE_LIMITED, decoded.failure().orElseThrow());
            assertEquals("8192", decoded.parsedBalance().orElseThrow().toString());
            assertTrue(decoded.isStructurallyValid());
        } finally {
            buffer.release();
        }
    }

    @Test
    void sessionPayloadRoundTripsAndRejectsInvalidAuthorityState() {
        AlchemySessionPayload original = new AlchemySessionPayload(1, 2, 7, "8192", 4);
        RegistryFriendlyByteBuf buffer = buffer();
        try {
            AlchemySessionPayload.CODEC.encode(buffer, original);
            AlchemySessionPayload decoded = AlchemySessionPayload.CODEC.decode(buffer);
            assertEquals(original, decoded);
            assertTrue(decoded.isStructurallyValid());
        } finally {
            buffer.release();
        }
        assertTrue(!new AlchemySessionPayload(1, 0, 7, "0", 0).isStructurallyValid());
        assertTrue(!new AlchemySessionPayload(99, 2, 7, "0", 0).isStructurallyValid());
        assertTrue(!new AlchemySessionPayload(1, 2, 7, "not-a-number", 0).isStructurallyValid());
    }

    @Test
    void malformedOperationsIdentifiersAndOversizedFieldsNeverBecomeTransactions() {
        assertTrue(new AlchemyActionPayload(1, 1, 0, 99, "minecraft:coal", 1, 0)
            .toTransaction().isEmpty());
        assertTrue(new AlchemyActionPayload(1, 1, 0, 1, "not an id", 1, 0)
            .toTransaction().isEmpty());
        assertTrue(new AlchemyActionPayload(1, 1, 0, 0, "minecraft:coal", 2, 0)
            .toTransaction().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new AlchemyActionPayload(
            1, 1, 0, 1, "x".repeat(AlchemyNetworkProtocol.MAX_ITEM_ID_LENGTH + 1), 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new AlchemyResultPayload(
            1, 1, 0, true, 0, 0, "9".repeat(AlchemyNetworkProtocol.MAX_BALANCE_LENGTH + 1), 0));
        assertThrows(IllegalArgumentException.class, () -> new AlchemyResultPayload(
            1, 1, 0, true, 0, 0, "0", -1));
        assertTrue(new AlchemyResultPayload(1, 1, 0, true, 999, 0, "0", 0)
            .failure().isEmpty());
        assertTrue(!new AlchemyResultPayload(1, 1, 0, true,
            AlchemyTransactionFailure.RATE_LIMITED.ordinal(), 0, "0", 0).isStructurallyValid());
    }

    private static RegistryFriendlyByteBuf buffer() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    }
}
