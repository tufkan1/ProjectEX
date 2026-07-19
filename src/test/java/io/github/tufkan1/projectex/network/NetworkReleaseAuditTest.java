package io.github.tufkan1.projectex.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tufkan1.projectex.alchemy.AlchemyTransactionFailure;
import io.github.tufkan1.projectex.audit.ReleaseAuditBudgets;
import io.github.tufkan1.projectex.teleport.AlchemicalDestination;
import io.netty.buffer.Unpooled;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;

class NetworkReleaseAuditTest {
    @Test
    void maximumPacketsStayInsidePublishedBudgets(TestReporter reporter) {
        AlchemyActionPayload action = new AlchemyActionPayload(
            1, Long.MAX_VALUE, Long.MAX_VALUE, Integer.MAX_VALUE,
            "a".repeat(AlchemyNetworkProtocol.MAX_ITEM_ID_LENGTH), Integer.MAX_VALUE, Long.MAX_VALUE);
        int actionBytes = encodedBytes(AlchemyActionPayload.CODEC, action);
        assertTrue(actionBytes <= ReleaseAuditBudgets.MAX_ACTION_PACKET_BYTES);

        String item = "audit:" + "a".repeat(AlchemyNetworkProtocol.MAX_ITEM_ID_LENGTH - 6);
        String emc = "9".repeat(AlchemyNetworkProtocol.MAX_EMC_VALUE_LENGTH);
        var entry = new AlchemyKnowledgePagePayload.Entry(item, emc);
        var page = new AlchemyKnowledgePagePayload(1, 1, 1,
            AlchemyTransactionFailure.NONE.ordinal(), 0, 1,
            AlchemyNetworkProtocol.MAX_KNOWLEDGE_PAGE_SIZE,
            java.util.Collections.nCopies(AlchemyNetworkProtocol.MAX_KNOWLEDGE_PAGE_SIZE, entry));
        assertTrue(page.isStructurallyValid());
        int pageBytes = encodedBytes(AlchemyKnowledgePagePayload.CODEC, page);
        assertTrue(pageBytes <= ReleaseAuditBudgets.MAX_KNOWLEDGE_PAGE_PACKET_BYTES);

        var destination = new AlchemicalDestination("a".repeat(32), "minecraft:overworld",
            Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        var bookEntry = new AlchemicalBookViewPayload.Entry(destination,
            "9".repeat(AlchemicalBookViewPayload.MAX_COST_LENGTH));
        var book = new AlchemicalBookViewPayload(UUID.randomUUID(), Long.MAX_VALUE, 3, true,
            "9".repeat(AlchemyNetworkProtocol.MAX_BALANCE_LENGTH), "x".repeat(64),
            java.util.Collections.nCopies(64, bookEntry), Optional.of(bookEntry));
        int bookBytes = encodedBytes(AlchemicalBookViewPayload.CODEC, book);
        assertTrue(bookBytes <= ReleaseAuditBudgets.MAX_ALCHEMICAL_BOOK_PACKET_BYTES);
        reporter.publishEntry(Map.of("actionBytes", Integer.toString(actionBytes),
            "knowledgePageBytes", Integer.toString(pageBytes), "alchemicalBookBytes", Integer.toString(bookBytes)));
    }

    @Test
    void oversizedAggregateFieldsAreRejectedBeforeEncoding() {
        assertThrows(IllegalArgumentException.class, () -> new AlchemyKnowledgePagePayload.Entry(
            "minecraft:coal", "9".repeat(AlchemyNetworkProtocol.MAX_EMC_VALUE_LENGTH + 1)));
        var destination = new AlchemicalDestination("home", "minecraft:overworld", 0, 64, 0);
        assertThrows(IllegalArgumentException.class, () -> new AlchemicalBookViewPayload.Entry(
            destination, "9".repeat(AlchemicalBookViewPayload.MAX_COST_LENGTH + 1)));
    }

    @Test
    void wireLimiterBoundsEverySupportedPlayerAndRecoversAfterLatencyWindow() {
        NetworkRequestLimiter limiter = new NetworkRequestLimiter();
        for (int playerIndex = 0; playerIndex < ReleaseAuditBudgets.SUPPORTED_PLAYERS; playerIndex++) {
            UUID player = new UUID(0, playerIndex + 1L);
            for (int request = 0; request < NetworkRequestLimiter.MAX_REQUESTS_PER_WINDOW; request++) {
                assertTrue(limiter.allow(player, 0));
            }
            assertFalse(limiter.allow(player, 999));
            assertTrue(limiter.allow(player, 1_001));
            limiter.disconnect(player);
            assertTrue(limiter.allow(player, 1_001));
        }
    }

    @Test
    void deterministicPacketFuzzNeverTurnsMalformedInputIntoAuthority() {
        java.util.Random random = new java.util.Random(0x50584c);
        for (int index = 0; index < 10_000; index++) {
            String item = switch (index % 4) {
                case 0 -> "not an id";
                case 1 -> "minecraft:";
                case 2 -> "minecraft:coal";
                default -> "../escape";
            };
            var payload = new AlchemyActionPayload(random.nextInt(), random.nextLong(), random.nextLong(),
                random.nextInt(), item, random.nextInt(), random.nextLong());
            if (payload.toTransaction().isPresent()) {
                assertEquals("minecraft:coal", item);
                assertTrue(payload.operationId() >= 0 && payload.operationId() <= 2);
            }
        }
    }

    private static <T> int encodedBytes(net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, T> codec,
                                        T payload) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            codec.encode(buffer, payload);
            return buffer.readableBytes();
        } finally {
            buffer.release();
        }
    }
}
