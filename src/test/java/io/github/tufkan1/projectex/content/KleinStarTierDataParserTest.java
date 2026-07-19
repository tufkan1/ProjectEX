package io.github.tufkan1.projectex.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.io.StringReader;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class KleinStarTierDataParserTest {
    @Test
    void parsesCompleteArbitraryPrecisionProgression() {
        String json = table((tier, index) -> BigInteger.TEN.pow(index + 1).toString());
        var parsed = KleinStarTierDataParser.parse(new StringReader(json));

        assertEquals(KleinStarTier.values().length, parsed.size());
        assertEquals(
            new EmcValue(BigInteger.TEN.pow(24)),
            parsed.get(KleinStarTier.GARGANTUAN_OMEGA)
        );
    }

    @Test
    void rejectsIncompleteDuplicateAndNonIncreasingTables() {
        String complete = table((tier, index) -> BigInteger.TEN.pow(index + 1).toString());
        String missing = complete.replace(
            ",{\"id\":\"gargantuan_star_omega\",\"capacity\":\"1000000000000000000000000\"}", ""
        );
        String duplicate = complete.replace(
            "{\"id\":\"klein_star_zwei\"", "{\"id\":\"klein_star_ein\""
        );
        String descending = table((tier, index) -> index == 10 ? "1" : BigInteger.TEN.pow(index + 1).toString());

        assertThrows(IllegalArgumentException.class, () -> KleinStarTierDataParser.parse(new StringReader(missing)));
        assertThrows(IllegalArgumentException.class, () -> KleinStarTierDataParser.parse(new StringReader(duplicate)));
        assertThrows(IllegalArgumentException.class, () -> KleinStarTierDataParser.parse(new StringReader(descending)));
    }

    @Test
    void rejectsUnknownFieldsAndNonCanonicalCapacity() {
        String unknown = table((tier, index) -> BigInteger.TEN.pow(index + 1).toString())
            .replace("\"schema_version\":1", "\"schema_version\":1,\"unsafe\":true");
        String leadingZero = table((tier, index) -> index == 0 ? "050000" : BigInteger.TEN.pow(index + 1).toString());

        assertThrows(IllegalArgumentException.class, () -> KleinStarTierDataParser.parse(new StringReader(unknown)));
        assertThrows(IllegalArgumentException.class, () -> KleinStarTierDataParser.parse(new StringReader(leadingZero)));
    }

    private static String table(Capacity capacity) {
        StringBuilder json = new StringBuilder("{\"schema_version\":1,\"tiers\":[");
        KleinStarTier[] tiers = KleinStarTier.values();
        for (int index = 0; index < tiers.length; index++) {
            if (index > 0) json.append(',');
            json.append("{\"id\":\"").append(tiers[index].serializedName())
                .append("\",\"capacity\":\"").append(capacity.value(tiers[index], index)).append("\"}");
        }
        return json.append("]}").toString();
    }

    @FunctionalInterface private interface Capacity {
        String value(KleinStarTier tier, int index);
    }
}
