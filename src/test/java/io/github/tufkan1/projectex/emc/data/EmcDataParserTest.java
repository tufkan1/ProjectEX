package io.github.tufkan1.projectex.emc.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class EmcDataParserTest {
    private static final List<String> INVALID_FIXTURES = List.of(
        "negative.json",
        "multiple-operations.json",
        "unsupported-version.json",
        "duplicate-components.json",
        "priority.json",
        "remove-false.json",
        "unknown-field.json"
    );

    @Test
    void parsesExplicitValuesWithDefaults() throws IOException {
        EmcDataFile file = parse("valid/explicit.json");

        assertEquals(1, file.schemaVersion());
        assertEquals(0, file.priority());
        assertEquals(2, file.values().size());
        assertEquals(EmcValue.of(8192), file.values().get(1).value());
    }

    @Test
    void parsesAllOperationsAndArbitraryPrecision() throws IOException {
        EmcDataFile file = parse("valid/operations.json");

        assertEquals(100, file.priority());
        assertEquals(EmcDefinition.Kind.ALIAS, file.values().get(0).kind());
        assertEquals(EmcDefinition.Kind.REMOVE, file.values().get(1).kind());
        assertEquals(new BigInteger("10000000000000000000000000000000000000000"),
            file.values().get(2).value().amount());
        assertEquals("{\"example:charge\":100,\"example:mode\":\"stable\"}",
            file.values().get(2).componentsJson());
    }

    @Test
    void bundledVanillaBaseValuesRemainValid() throws IOException {
        String resource = "/data/projectex/projectex/emc/vanilla_base.json";
        try (InputStream stream = EmcDataParserTest.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IOException("Missing bundled EMC data: " + resource);
            }
            EmcDataFile file = EmcDataParser.parse(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
            assertEquals(67, file.values().size());
            assertEquals(EmcValue.of(8192), file.values().stream()
                .filter(value -> value.item().toString().equals("minecraft:diamond"))
                .findFirst().orElseThrow().value());
        }
    }

    @TestFactory
    Stream<DynamicTest> rejectsInvalidFixtures() {
        return INVALID_FIXTURES.stream().map(name -> DynamicTest.dynamicTest(name,
            () -> assertThrows(EmcDataException.class, () -> parse("invalid/" + name))));
    }

    private static EmcDataFile parse(String fixture) throws IOException {
        String resource = "/emc-schema/" + fixture;
        try (InputStream stream = EmcDataParserTest.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IOException("Missing test fixture: " + resource);
            }
            return EmcDataParser.parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
    }
}
