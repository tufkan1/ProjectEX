package io.github.tufkan1.projectex.content;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.api.storage.EmcAutomationPolicy;

/** Fixed content identities for the continuous, data-balanced portable EMC progression. */
public enum KleinStarTier {
    EIN("klein_star_ein", "50000"),
    ZWEI("klein_star_zwei", "200000"),
    DREI("klein_star_drei", "800000"),
    VIER("klein_star_vier", "3200000"),
    SPHERE("klein_star_sphere", "12800000"),
    OMEGA("klein_star_omega", "51200000"),
    MAGNUM_EIN("magnum_star_ein", "204800000"),
    MAGNUM_ZWEI("magnum_star_zwei", "819200000"),
    MAGNUM_DREI("magnum_star_drei", "3276800000"),
    MAGNUM_VIER("magnum_star_vier", "13107200000"),
    MAGNUM_SPHERE("magnum_star_sphere", "52428800000"),
    MAGNUM_OMEGA("magnum_star_omega", "209715200000"),
    COLOSSAL_EIN("colossal_star_ein", "838860800000"),
    COLOSSAL_ZWEI("colossal_star_zwei", "3355443200000"),
    COLOSSAL_DREI("colossal_star_drei", "13421772800000"),
    COLOSSAL_VIER("colossal_star_vier", "53687091200000"),
    COLOSSAL_SPHERE("colossal_star_sphere", "214748364800000"),
    COLOSSAL_OMEGA("colossal_star_omega", "858993459200000"),
    GARGANTUAN_EIN("gargantuan_star_ein", "3435973836800000"),
    GARGANTUAN_ZWEI("gargantuan_star_zwei", "13743895347200000"),
    GARGANTUAN_DREI("gargantuan_star_drei", "54975581388800000"),
    GARGANTUAN_VIER("gargantuan_star_vier", "219902325555200000"),
    GARGANTUAN_SPHERE("gargantuan_star_sphere", "879609302220800000"),
    GARGANTUAN_OMEGA("gargantuan_star_omega", "3518437208883200000");

    private final String serializedName;
    private final EmcValue capacity;

    KleinStarTier(String serializedName, String capacity) {
        this.serializedName = serializedName;
        this.capacity = new EmcValue(new java.math.BigInteger(capacity));
    }

    public String serializedName() {
        return serializedName;
    }

    public EmcValue capacity() {
        return KleinStarTierConfig.capacity(this);
    }

    public EmcValue defaultCapacity() { return capacity; }

    public EmcAutomationPolicy automationPolicy() {
        return EmcAutomationPolicy.INPUT_OUTPUT;
    }

    public KleinStarTier next() {
        return ordinal() + 1 < values().length ? values()[ordinal() + 1] : null;
    }
}
