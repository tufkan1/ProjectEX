package io.github.tufkan1.projectex.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerAlchemyStateCodecTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void roundTripsDeterministicallyAndDefensivelyCopiesKnowledge() throws IOException {
        SortedMap<UUID, PlayerAlchemyState> decoded = PlayerAlchemyStateCodec.decode(fixture("v1.json"));

        assertEquals(EmcValue.of(8192), decoded.get(PLAYER).balance());
        assertTrue(decoded.get(PLAYER).knows(EmcKey.parse("minecraft:diamond")));
        assertEquals(PlayerAlchemyStateCodec.encode(decoded), PlayerAlchemyStateCodec.encode(decoded));
        assertThrows(UnsupportedOperationException.class,
            () -> decoded.get(PLAYER).knowledge().add(EmcKey.parse("minecraft:stick")));
    }

    @Test
    void migratesVersionZeroEmcFieldToVersionOneBalance() throws IOException {
        SortedMap<UUID, PlayerAlchemyState> decoded = PlayerAlchemyStateCodec.decode(fixture("v0.json"));
        String migrated = PlayerAlchemyStateCodec.encode(decoded);

        assertEquals(EmcValue.of(128), decoded.get(PLAYER).balance());
        assertTrue(migrated.contains("\"schema_version\":1"));
        assertTrue(migrated.contains("\"balance\":\"128\""));
        assertFalse(migrated.contains("\"emc\""));
    }

    @Test
    void rejectsNegativeCorruptAndOverLimitBalances() throws IOException {
        assertThrows(PlayerStateDataException.class,
            () -> PlayerAlchemyStateCodec.decode(fixture("invalid-negative.json")));

        String maximum = "9".repeat(PlayerAlchemyState.MAX_BALANCE_DIGITS);
        PlayerAlchemyState state = new PlayerAlchemyState(new EmcValue(new BigInteger(maximum)), new TreeSet<>());
        assertEquals(maximum, state.balance().amount().toString());
        assertThrows(IllegalArgumentException.class, () -> new PlayerAlchemyState(
            new EmcValue(new BigInteger("9".repeat(PlayerAlchemyState.MAX_BALANCE_DIGITS + 1))),
            new TreeSet<>()
        ));
    }

    @Test
    void creditDebitAndKnowledgeChangesAreImmutable() {
        EmcKey diamond = EmcKey.parse("minecraft:diamond");
        PlayerAlchemyState original = PlayerAlchemyState.EMPTY;
        PlayerAlchemyState credited = original.credit(EmcValue.of(8192)).learn(diamond);

        assertEquals(PlayerAlchemyState.EMPTY, original);
        assertTrue(credited.knows(diamond));
        assertEquals(EmcValue.ZERO, credited.debit(EmcValue.of(8192)).orElseThrow().balance());
        assertTrue(credited.debit(EmcValue.of(8193)).isEmpty());
    }

    private static String fixture(String name) throws IOException {
        try (var stream = PlayerAlchemyStateCodecTest.class.getResourceAsStream("/player-state/" + name)) {
            if (stream == null) {
                throw new IOException("Missing fixture: " + name);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
