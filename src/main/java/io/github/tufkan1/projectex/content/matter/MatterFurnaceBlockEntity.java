package io.github.tufkan1.projectex.content.matter;

import io.github.tufkan1.projectex.content.MatterFurnaceBlock;
import io.github.tufkan1.projectex.content.ProjectEXBlockEntities;
import io.github.tufkan1.projectex.matter.MatterFurnaceTransaction;
import io.github.tufkan1.projectex.matter.MatterTier;
import io.github.tufkan1.projectex.matter.MatterTierConfig;
import io.github.tufkan1.projectex.storage.CondenserVariant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import io.github.tufkan1.projectex.menu.MatterFurnaceMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Persistent exact-inventory runtime for the two matter furnace tiers. */
public final class MatterFurnaceBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    public static final int INPUT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int OUTPUT_START = 2;
    private static final int FORMAT_VERSION = 1;

    private final MatterTier tier;
    private final NonNullList<ItemStack> items;
    private int burnRemaining;
    private int burnTotal;
    private int cookProgress;

    public MatterFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ProjectEXBlockEntities.MATTER_FURNACE, pos, state);
        if (!(state.getBlock() instanceof MatterFurnaceBlock furnace)) {
            throw new IllegalArgumentException("Matter furnace requires its registered block");
        }
        tier = furnace.tier();
        items = NonNullList.withSize(27, ItemStack.EMPTY);
    }

    public MatterTier tier() { return MatterTierConfig.resolve(tier); }
    public int burnRemaining() { return burnRemaining; }
    public int burnTotal() { return burnTotal; }
    public int cookProgress() { return cookProgress; }

    public static void tickServer(
        net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
        MatterFurnaceBlockEntity furnace
    ) {
        if (level instanceof ServerLevel serverLevel) furnace.tick(serverLevel);
    }

    private void tick(ServerLevel level) {
        boolean changed = false;
        if (burnRemaining > 0) {
            burnRemaining--;
            changed = true;
        }

        ItemStack input = items.get(INPUT_SLOT);
        var recipe = input.isEmpty() ? Optional.<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.SmeltingRecipe>>empty() : level.recipeAccess().getRecipeFor(
            RecipeType.SMELTING, new SingleRecipeInput(input), level
        );
        ItemStack recipeOutput = recipe.map(holder -> holder.value().assemble(new SingleRecipeInput(input)))
            .orElse(ItemStack.EMPTY);
        MatterTier currentTier = tier();
        boolean canSmelt = !recipeOutput.isEmpty() && evaluate(currentTier, recipeOutput, true).committed();

        if (burnRemaining == 0 && canSmelt && ignite(level)) changed = true;
        if (burnRemaining > 0 && canSmelt) {
            cookProgress++;
            changed = true;
            if (cookProgress >= currentTier.furnaceCookTicks()) {
                boolean bonus = level.getRandom().nextInt(currentTier.bonusOutputDenominator())
                    < currentTier.bonusOutputNumerator();
                var result = evaluate(currentTier, recipeOutput, bonus);
                if (result.committed()) applySmelt(result, recipeOutput);
                cookProgress = 0;
            }
        } else if (cookProgress > 0) {
            cookProgress = Math.max(0, cookProgress - 2);
            changed = true;
        }
        if (changed) changed();
    }

    private boolean ignite(ServerLevel level) {
        ItemStack fuel = items.get(FUEL_SLOT);
        int burnTicks = level.fuelValues().burnDuration(fuel);
        net.minecraft.world.item.ItemStackTemplate remainder = fuel.getItem().getCraftingRemainder();
        Optional<CondenserVariant> remainderVariant = remainder == null
            ? Optional.empty() : Optional.of(variant(remainder.create()));
        var result = MatterFurnaceTransaction.ignite(
            fuel.getCount(), burnTicks, remainderVariant, remainder == null || fuel.getCount() == 1
        );
        if (!result.committed()) return false;
        burnRemaining = result.burnTicks();
        burnTotal = result.burnTicks();
        if (remainder == null) fuel.shrink(1);
        else items.set(FUEL_SLOT, remainder.create());
        return true;
    }

    private MatterFurnaceTransaction.SmeltResult evaluate(
        MatterTier currentTier, ItemStack output, boolean bonus
    ) {
        List<MatterFurnaceTransaction.OutputSlot> slots = new ArrayList<>(currentTier.furnaceOutputSlots());
        for (int index = 0; index < currentTier.furnaceOutputSlots(); index++) {
            ItemStack stack = items.get(OUTPUT_START + index);
            slots.add(stack.isEmpty()
                ? MatterFurnaceTransaction.OutputSlot.empty(output.getMaxStackSize())
                : new MatterFurnaceTransaction.OutputSlot(
                    Optional.of(variant(stack)), stack.getCount(), stack.getMaxStackSize()
                ));
        }
        return MatterFurnaceTransaction.smelt(
            currentTier, items.get(INPUT_SLOT).getCount(),
            new MatterFurnaceTransaction.Output(variant(output), output.getCount()), slots, bonus
        );
    }

    private void applySmelt(MatterFurnaceTransaction.SmeltResult result, ItemStack recipeOutput) {
        items.get(INPUT_SLOT).shrink(1);
        for (int index = 0; index < result.outputs().size(); index++) {
            var planned = result.outputs().get(index);
            ItemStack existing = items.get(OUTPUT_START + index);
            if (planned.variant().isEmpty()) {
                items.set(OUTPUT_START + index, ItemStack.EMPTY);
            } else if (!existing.isEmpty() && variant(existing).equals(planned.variant().get())) {
                existing.setCount(planned.count());
            } else if (variant(recipeOutput).equals(planned.variant().get())) {
                items.set(OUTPUT_START + index, recipeOutput.copyWithCount(planned.count()));
            } else {
                throw new IllegalStateException("Matter furnace output plan lost its component variant");
            }
        }
    }

    private static CondenserVariant variant(ItemStack stack) {
        return new CondenserVariant(
            BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
            stack.getComponentsPatch().isEmpty() ? null : stack.getComponentsPatch().toString()
        );
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MatterFurnaceMenu(id, inventory, this);
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int version = input.getIntOr("matter_furnace_version", FORMAT_VERSION);
        if (version == FORMAT_VERSION) {
            burnRemaining = Math.max(0, input.getIntOr("burn_remaining", 0));
            burnTotal = Math.max(0, input.getIntOr("burn_total", 0));
            cookProgress = Math.max(0, Math.min(tier().furnaceCookTicks(), input.getIntOr("cook_progress", 0)));
        }
        ContainerHelper.loadAllItems(input, items);
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("matter_furnace_version", FORMAT_VERSION);
        output.putInt("burn_remaining", burnRemaining);
        output.putInt("burn_total", burnTotal);
        output.putInt("cook_progress", cookProgress);
        ContainerHelper.saveAllItems(output, items);
    }
    @Override protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        components.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
    }
    @Override protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }

    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int count) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, count);
        if (!removed.isEmpty()) changed();
        return removed;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize(stack)) stack.setCount(getMaxStackSize(stack));
        changed();
    }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { items.clear(); changed(); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == INPUT_SLOT) return true;
        if (slot == FUEL_SLOT) return level != null && level.fuelValues().isFuel(stack);
        return false;
    }
    @Override public int[] getSlotsForFace(Direction side) {
        if (side == Direction.UP) return new int[] { INPUT_SLOT };
        if (side == Direction.DOWN) return java.util.stream.IntStream.concat(
            java.util.stream.IntStream.of(FUEL_SLOT),
            java.util.stream.IntStream.range(OUTPUT_START, OUTPUT_START + tier().furnaceOutputSlots())
        ).toArray();
        return new int[] { FUEL_SLOT };
    }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return Arrays.stream(getSlotsForFace(side)).anyMatch(value -> value == slot) && canPlaceItem(slot, stack);
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side == Direction.DOWN && (
            (slot == FUEL_SLOT && stack.is(net.minecraft.world.item.Items.BUCKET))
                || (slot >= OUTPUT_START && slot < OUTPUT_START + tier().furnaceOutputSlots())
        );
    }
}
