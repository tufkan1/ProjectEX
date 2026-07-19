package io.github.tufkan1.projectex.matter;

/** Bounded armor calculation that cannot produce invulnerability or client-only state. */
public final class MatterArmorPolicy {
    private MatterArmorPolicy() {
    }

    public static Result evaluate(
        MatterTier tier,
        int equippedPieces,
        double incomingDamage,
        boolean bypassesArmor,
        long currentTick,
        long lastPeriodicEffectTick
    ) {
        if (equippedPieces < 0 || equippedPieces > 4 || incomingDamage < 0
            || currentTick < 0 || lastPeriodicEffectTick < -1) {
            throw new IllegalArgumentException("Invalid matter armor input");
        }
        double reduction = bypassesArmor ? 0
            : tier.armorDamageReductionCap() * equippedPieces / 4.0;
        reduction = Math.min(reduction, 0.95);
        double resulting = incomingDamage * (1.0 - reduction);
        boolean periodicEffect = equippedPieces == 4
            && (lastPeriodicEffectTick < 0 || currentTick - lastPeriodicEffectTick >= 20);
        return new Result(reduction, resulting, periodicEffect);
    }

    public record Result(double reductionFraction, double resultingDamage, boolean periodicEffectAllowed) {
        public Result {
            if (reductionFraction < 0 || reductionFraction > 0.95 || resultingDamage < 0) {
                throw new IllegalArgumentException("Unsafe armor result");
            }
        }
    }
}
