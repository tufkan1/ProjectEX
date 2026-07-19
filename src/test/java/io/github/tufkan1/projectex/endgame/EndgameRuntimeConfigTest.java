package io.github.tufkan1.projectex.endgame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.api.endgame.FinalStarSlot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EndgameRuntimeConfigTest {
    @BeforeEach void initializeWithDefaults() {
        System.clearProperty(EndgameRuntimeConfig.FINAL_STAR_ENABLED);
        System.clearProperty(EndgameRuntimeConfig.FINAL_STAR_SLOTS);
        System.clearProperty(EndgameRuntimeConfig.FINAL_STAR_COOLDOWN);
        System.clearProperty(EndgameRuntimeConfig.CONSUMABLES_ENABLED);
        System.clearProperty(EndgameRuntimeConfig.STEAK_COST);
        System.clearProperty(EndgameRuntimeConfig.STEAK_COOLDOWN);
        EndgameRuntimeConfig.reload();
    }

    @AfterEach void clear() {
        System.clearProperty(EndgameRuntimeConfig.FINAL_STAR_ENABLED);
        System.clearProperty(EndgameRuntimeConfig.FINAL_STAR_SLOTS);
        System.clearProperty(EndgameRuntimeConfig.FINAL_STAR_COOLDOWN);
        System.clearProperty(EndgameRuntimeConfig.CONSUMABLES_ENABLED);
        System.clearProperty(EndgameRuntimeConfig.STEAK_COST);
        System.clearProperty(EndgameRuntimeConfig.STEAK_COOLDOWN);
        EndgameRuntimeConfig.reload();
    }

    @Test void parsesExplicitGatesSlotsCooldownsAndArbitraryPrecisionCost() {
        System.setProperty(EndgameRuntimeConfig.FINAL_STAR_ENABLED, "false");
        System.setProperty(EndgameRuntimeConfig.FINAL_STAR_SLOTS, "off_hand,inventory");
        System.setProperty(EndgameRuntimeConfig.FINAL_STAR_COOLDOWN, "37");
        System.setProperty(EndgameRuntimeConfig.CONSUMABLES_ENABLED, "false");
        System.setProperty(EndgameRuntimeConfig.STEAK_COST, "123456789012345678901234567890");
        System.setProperty(EndgameRuntimeConfig.STEAK_COOLDOWN, "41");

        var config = EndgameRuntimeConfig.load();
        assertFalse(config.finalStarEnabled());
        assertEquals(java.util.Set.of(FinalStarSlot.OFF_HAND, FinalStarSlot.INVENTORY), config.finalStarSlots());
        assertEquals(37, config.finalStarCooldownTicks());
        assertFalse(config.infiniteConsumablesEnabled());
        assertEquals(new EmcValue(new java.math.BigInteger("123456789012345678901234567890")),
            config.infiniteSteakCost());
        assertEquals(41, config.infiniteSteakCooldownTicks());
    }

    @Test void rejectsUnknownSlotsInvalidBooleansAndUnsafeBounds() {
        System.setProperty(EndgameRuntimeConfig.FINAL_STAR_SLOTS, "armor");
        assertThrows(IllegalArgumentException.class, EndgameRuntimeConfig::load);
        System.clearProperty(EndgameRuntimeConfig.FINAL_STAR_SLOTS);
        System.setProperty(EndgameRuntimeConfig.FINAL_STAR_ENABLED, "yes");
        assertThrows(IllegalArgumentException.class, EndgameRuntimeConfig::load);
        System.clearProperty(EndgameRuntimeConfig.FINAL_STAR_ENABLED);
        System.setProperty(EndgameRuntimeConfig.STEAK_COOLDOWN, "0");
        assertThrows(IllegalArgumentException.class, EndgameRuntimeConfig::load);
        System.clearProperty(EndgameRuntimeConfig.STEAK_COOLDOWN);
        System.setProperty(EndgameRuntimeConfig.STEAK_COST, "064");
        assertThrows(IllegalArgumentException.class, EndgameRuntimeConfig::load);
    }
}
