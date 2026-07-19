package io.github.tufkan1.projectex.matter;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.util.Map;

/** Immutable, data-ready balance definition shared by matter blocks, tools, armor, and furnaces. */
public record MatterTier(
    String id,
    int miningLevel,
    double miningSpeed,
    double attackBonus,
    int maxCharge,
    int maxAreaBlocks,
    int actionCooldownTicks,
    EmcValue emcPerAreaBlock,
    int furnaceCookTicks,
    int furnaceOutputSlots,
    int bonusOutputNumerator,
    int bonusOutputDenominator,
    double armorDamageReductionCap
) {
    public static final MatterTier DARK = new MatterTier(
        "dark_matter", 3, 14.0, 3.0, 4, 125, 8, EmcValue.of(64),
        10, 9, 1, 2, 0.80
    );
    public static final MatterTier RED = new MatterTier(
        "red_matter", 4, 16.0, 4.0, 5, 343, 6, EmcValue.of(32),
        5, 18, 1, 1, 0.90
    );
    public static final Map<String, MatterTier> DEFAULTS = Map.of(DARK.id, DARK, RED.id, RED);

    public MatterTier {
        if (id == null || id.isBlank() || miningLevel < 0 || miningSpeed <= 0 || attackBonus < 0
            || maxCharge < 0 || maxCharge > 16 || maxAreaBlocks <= 0 || maxAreaBlocks > 4_096
            || actionCooldownTicks < 1 || furnaceCookTicks < 1 || furnaceOutputSlots < 1
            || bonusOutputNumerator < 0 || bonusOutputDenominator < 1
            || armorDamageReductionCap < 0 || armorDamageReductionCap > 0.95) {
            throw new IllegalArgumentException("Unsafe matter tier definition");
        }
        java.util.Objects.requireNonNull(emcPerAreaBlock, "emcPerAreaBlock");
    }

    public int radiusForCharge(int charge) {
        if (charge < 0 || charge > maxCharge) throw new IllegalArgumentException("Charge outside tier range");
        return charge;
    }
}
