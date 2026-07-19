package io.github.tufkan1.projectex.matter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.io.StringReader;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MatterTierDataParserTest {
    @AfterEach void restoreDefaults() { MatterTierConfig.publish(MatterTier.DEFAULTS); }

    @Test void completeSchemaParsesToValidatedTier() {
        MatterTier parsed = MatterTierDataParser.parse(new StringReader(json("dark_matter", 9)));
        assertEquals("dark_matter", parsed.id());
        assertEquals(9, parsed.furnaceOutputSlots());
        assertEquals(EmcValue.of(64), parsed.emcPerAreaBlock());
    }

    @Test void unknownMissingAndUnsafeFieldsFailClosed() {
        assertThrows(IllegalArgumentException.class, () -> MatterTierDataParser.parse(
            new StringReader(json("dark_matter", 9).replaceFirst("\\{", "{\"unknown\":1,"))
        ));
        assertThrows(IllegalArgumentException.class, () -> MatterTierDataParser.parse(
            new StringReader(json("dark_matter", 9).replace("\"max_charge\":4,", ""))
        ));
        assertThrows(IllegalArgumentException.class, () -> MatterTierDataParser.parse(
            new StringReader(json("dark_matter", 9).replace("\"bonus_output_numerator\":1",
                "\"bonus_output_numerator\":3"))
        ));
    }

    @Test void snapshotPublishesBothTiersAtomicallyAndRejectsRuntimeOverflow() {
        MatterTier changed = new MatterTier(
            "dark_matter", 3, 12, 3, 2, 27, 10, EmcValue.of(99),
            12, 9, 1, 2, 0.7
        );
        MatterTierConfig.publish(Map.of("dark_matter", changed, "red_matter", MatterTier.RED));
        assertEquals(changed, MatterTierConfig.resolve(MatterTier.DARK));
        assertThrows(IllegalArgumentException.class, () -> MatterTierConfig.publish(Map.of(
            "dark_matter", changed,
            "red_matter", new MatterTier("red_matter", 4, 16, 4, 5, 343, 6,
                EmcValue.of(32), 5, 19, 1, 1, 0.9)
        )));
        assertEquals(changed, MatterTierConfig.resolve(MatterTier.DARK));
    }

    private static String json(String id, int outputSlots) {
        return """
            {
              "schema_version":1,
              "id":"%s",
              "mining_level":3,
              "mining_speed":14.0,
              "attack_bonus":3.0,
              "max_charge":4,
              "max_area_blocks":125,
              "action_cooldown_ticks":8,
              "emc_per_area_block":"64",
              "furnace_cook_ticks":10,
              "furnace_output_slots":%d,
              "bonus_output_numerator":1,
              "bonus_output_denominator":2,
              "armor_damage_reduction_cap":0.8
            }
            """.formatted(id, outputSlots);
    }
}
