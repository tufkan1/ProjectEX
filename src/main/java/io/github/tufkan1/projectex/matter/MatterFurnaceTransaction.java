package io.github.tufkan1.projectex.matter;

import io.github.tufkan1.projectex.storage.CondenserVariant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Atomic furnace completion/fuel evaluator; callers commit the returned slot plan as one mutation. */
public final class MatterFurnaceTransaction {
    private MatterFurnaceTransaction() {
    }

    public static SmeltResult smelt(
        MatterTier tier,
        int inputCount,
        Output recipeOutput,
        List<OutputSlot> outputSlots,
        boolean bonusRollSucceeded
    ) {
        Objects.requireNonNull(tier, "tier");
        Objects.requireNonNull(recipeOutput, "recipeOutput");
        List<OutputSlot> slots = List.copyOf(outputSlots);
        if (inputCount <= 0 || recipeOutput.count <= 0) return SmeltResult.unchanged(inputCount, slots);
        int bonus = bonusRollSucceeded && tier.bonusOutputNumerator() > 0 ? recipeOutput.count : 0;
        int produced = Math.addExact(recipeOutput.count, bonus);
        int remaining = produced;
        List<OutputSlot> resulting = new ArrayList<>(slots);

        for (int index = 0; index < resulting.size() && remaining > 0; index++) {
            OutputSlot slot = resulting.get(index);
            if (slot.variant.isPresent() && slot.variant.get().equals(recipeOutput.variant)) {
                int moved = Math.min(remaining, slot.capacity - slot.count);
                resulting.set(index, new OutputSlot(slot.variant, slot.count + moved, slot.capacity));
                remaining -= moved;
            }
        }
        for (int index = 0; index < resulting.size() && remaining > 0; index++) {
            OutputSlot slot = resulting.get(index);
            if (slot.variant.isEmpty()) {
                int moved = Math.min(remaining, slot.capacity);
                resulting.set(index, new OutputSlot(Optional.of(recipeOutput.variant), moved, slot.capacity));
                remaining -= moved;
            }
        }
        return remaining == 0
            ? new SmeltResult(true, inputCount - 1, produced, resulting)
            : SmeltResult.unchanged(inputCount, slots);
    }

    /** Consumes fuel only when both its burn time and crafting remainder have an exact destination. */
    public static FuelResult ignite(
        int fuelCount,
        int burnTicksPerItem,
        Optional<CondenserVariant> craftingRemainder,
        boolean remainderSinkAvailable
    ) {
        Objects.requireNonNull(craftingRemainder, "craftingRemainder");
        if (fuelCount <= 0 || burnTicksPerItem <= 0) return new FuelResult(false, fuelCount, 0, Optional.empty());
        if (craftingRemainder.isPresent() && !remainderSinkAvailable) {
            return new FuelResult(false, fuelCount, 0, Optional.empty());
        }
        return new FuelResult(true, fuelCount - 1, burnTicksPerItem, craftingRemainder);
    }

    public record Output(CondenserVariant variant, int count) {
        public Output { Objects.requireNonNull(variant, "variant"); }
    }

    public record OutputSlot(Optional<CondenserVariant> variant, int count, int capacity) {
        public OutputSlot {
            Objects.requireNonNull(variant, "variant");
            if (count < 0 || capacity < 1 || count > capacity || (count == 0) != variant.isEmpty()) {
                throw new IllegalArgumentException("Invalid matter furnace output slot");
            }
        }
        public static OutputSlot empty(int capacity) { return new OutputSlot(Optional.empty(), 0, capacity); }
    }

    public record SmeltResult(boolean committed, int resultingInputCount, int produced, List<OutputSlot> outputs) {
        public SmeltResult { outputs = List.copyOf(outputs); }
        private static SmeltResult unchanged(int inputCount, List<OutputSlot> slots) {
            return new SmeltResult(false, Math.max(0, inputCount), 0, slots);
        }
    }

    public record FuelResult(
        boolean committed,
        int resultingFuelCount,
        int burnTicks,
        Optional<CondenserVariant> craftingRemainder
    ) {
        public FuelResult { Objects.requireNonNull(craftingRemainder, "craftingRemainder"); }
    }
}
