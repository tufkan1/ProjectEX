package io.github.tufkan1.projectex.teleport;

import io.github.tufkan1.projectex.api.emc.EmcValue;
import java.math.BigInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Rarity;

/** ProjectExpansion-compatible teleport capabilities and exact EMC ratios. */
public enum AlchemicalBookTier {
    BASIC(1_000, false, false, Rarity.COMMON),
    ADVANCED(500, true, false, Rarity.UNCOMMON),
    MASTER(100, true, true, Rarity.RARE),
    ARCANE(0, true, true, Rarity.EPIC);

    private final int emcPerBlock;
    private final boolean bindable;
    private final boolean crossDimension;
    private final Rarity rarity;

    AlchemicalBookTier(int emcPerBlock, boolean bindable, boolean crossDimension, Rarity rarity) {
        this.emcPerBlock = emcPerBlock;
        this.bindable = bindable;
        this.crossDimension = crossDimension;
        this.rarity = rarity;
    }

    public int emcPerBlock() { return emcPerBlock; }
    public boolean bindable() { return bindable; }
    public boolean crossDimension() { return crossDimension; }
    public Rarity rarity() { return rarity; }

    public EmcValue cost(BlockPos origin, AlchemicalDestination destination, boolean creative) {
        if (creative || emcPerBlock == 0) return EmcValue.ZERO;
        double dx = (double) origin.getX() - destination.x();
        double dy = (double) origin.getY() - destination.y();
        double dz = (double) origin.getZ() - destination.z();
        long blocks = (long) Math.ceil(Math.sqrt(dx * dx + dy * dy + dz * dz));
        return new EmcValue(BigInteger.valueOf(blocks).multiply(BigInteger.valueOf(emcPerBlock)));
    }
}
