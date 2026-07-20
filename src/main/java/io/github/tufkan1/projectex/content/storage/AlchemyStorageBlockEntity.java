package io.github.tufkan1.projectex.content.storage;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.api.fabric.MinecraftEmcAdapter;
import io.github.tufkan1.projectex.content.AlchemyStorageBlock;
import io.github.tufkan1.projectex.content.ProjectEXBlockEntities;
import io.github.tufkan1.projectex.content.ProjectEXComponents;
import io.github.tufkan1.projectex.content.component.AlchemyStorageState;
import io.github.tufkan1.projectex.menu.AlchemyStorageMenu;
import io.github.tufkan1.projectex.storage.CondenserTransaction;
import io.github.tufkan1.projectex.storage.CondenserVariant;
import io.github.tufkan1.projectex.storage.StorageKind;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Exact, versioned inventory implementation for condensers and alchemical storage. */
public final class AlchemyStorageBlockEntity extends BlockEntity
    implements WorldlyContainer, MenuProvider, ExtendedMenuProvider<Integer> {
    public static final int TARGET_SLOT = 0;

    private final StorageKind kind;
    private final NonNullList<ItemStack> items;
    private AlchemyStorageState state = AlchemyStorageState.empty();

    public AlchemyStorageBlockEntity(BlockPos pos, BlockState blockState) {
        super(ProjectEXBlockEntities.ALCHEMY_STORAGE, pos, blockState);
        if (!(blockState.getBlock() instanceof AlchemyStorageBlock block)) {
            throw new IllegalArgumentException("Alchemy storage requires its registered block");
        }
        kind = block.kind();
        items = NonNullList.withSize(kind.inventorySlots(), ItemStack.EMPTY);
    }

    public static void tickServer(
        net.minecraft.world.level.Level level, BlockPos pos, BlockState blockState,
        AlchemyStorageBlockEntity storage
    ) {
        if (level instanceof ServerLevel) storage.processCondenser();
    }

    public StorageKind kind() { return kind; }

    @Override public Integer getScreenOpeningData(ServerPlayer player) {
        return AlchemyStorageMenu.openingData(kind, false);
    }
    public AlchemyStorageState storageState() { return state; }

    public void claim(UUID owner) {
        state = new AlchemyStorageState(
            state.version(), state.stored(), state.access().claim(owner), state.advancedConfig()
        );
        changed();
    }

    public boolean canUse(Player player) {
        return state.access().permits(
            player.getUUID(), player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)
        ) && Container.stillValidBlockEntity(this, player);
    }

    public boolean setPublicAccess(boolean enabled, Player actor) {
        boolean operator = actor.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        if (!state.access().permits(actor.getUUID(), operator)) return false;
        state = new AlchemyStorageState(
            state.version(), state.stored(),
            state.access().withPublicAccess(enabled, actor.getUUID(), operator), state.advancedConfig()
        );
        changed();
        return true;
    }

    private void processCondenser() {
        if (!kind.condenser()) return;
        ItemStack target = items.get(TARGET_SLOT);
        EmcValue targetValue = valueOf(target);
        if (target.isEmpty() || targetValue == null || targetValue.equals(EmcValue.ZERO)) return;

        CondenserVariant targetVariant = variant(target);
        List<CondenserTransaction.Input> inputs = new ArrayList<>(kind.inputSlots());
        for (int slot = kind.inputStart(); slot < kind.inputEnd(); slot++) {
            ItemStack stack = items.get(slot);
            EmcValue value = valueOf(stack);
            if (kind.sharedOutput() && ItemStack.isSameItemSameComponents(stack, target)) {
                value = EmcValue.ZERO;
            }
            inputs.add(new CondenserTransaction.Input(
                variant(stack), value == null ? EmcValue.ZERO : value, stack.getCount()
            ));
        }
        int room = outputRoom(target);
        CondenserTransaction.Result result = CondenserTransaction.evaluate(
            targetVariant, targetValue, state.stored(), inputs, room, kind.inputBudget(), true
        );
        if (!result.changed()) return;

        for (int index = 0; index < result.consumedCounts().size(); index++) {
            int consumed = result.consumedCounts().get(index);
            if (consumed > 0) items.get(kind.inputStart() + index).shrink(consumed);
        }
        insertOutput(target, result.produced());
        state = new AlchemyStorageState(
            state.version(), result.stored(), state.access(), state.advancedConfig()
        );
        changed();
    }

    public boolean cycleAdvancedFilter(Player actor) {
        if (kind != StorageKind.ADVANCED_ALCHEMICAL_CHEST || !canUse(actor)) return false;
        state = new AlchemyStorageState(
            state.version(), state.stored(), state.access(), state.advancedConfig().cycleMode()
        );
        changed();
        return true;
    }

    public boolean toggleAdvancedFilter(ItemStack stack, Player actor) {
        if (kind != StorageKind.ADVANCED_ALCHEMICAL_CHEST || stack.isEmpty() || !canUse(actor)) return false;
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        var updated = state.advancedConfig().toggle(itemId);
        if (updated.equals(state.advancedConfig())) return false;
        state = new AlchemyStorageState(state.version(), state.stored(), state.access(), updated);
        changed();
        return true;
    }

    public void sortAdvanced() {
        if (kind != StorageKind.ADVANCED_ALCHEMICAL_CHEST) return;
        List<ItemStack> sorted = items.stream().filter(stack -> !stack.isEmpty()).map(ItemStack::copy)
            .sorted(java.util.Comparator
                .comparing((ItemStack stack) -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
                .thenComparing(stack -> stack.getComponentsPatch().toString()))
            .toList();
        boolean changed = false;
        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack replacement = slot < sorted.size() ? sorted.get(slot) : ItemStack.EMPTY;
            if (!ItemStack.matches(items.get(slot), replacement)) changed = true;
            items.set(slot, replacement);
        }
        if (changed) changed();
    }

    private int outputRoom(ItemStack target) {
        int room = 0;
        for (int slot = kind.outputStart(); slot < kind.outputEnd(); slot++) {
            ItemStack output = items.get(slot);
            if (output.isEmpty()) room += target.getMaxStackSize();
            else if (ItemStack.isSameItemSameComponents(output, target)) {
                room += output.getMaxStackSize() - output.getCount();
            }
        }
        return room;
    }

    private void insertOutput(ItemStack target, int count) {
        int remaining = count;
        for (int slot = kind.outputStart(); slot < kind.outputEnd() && remaining > 0; slot++) {
            ItemStack output = items.get(slot);
            if (!output.isEmpty() && ItemStack.isSameItemSameComponents(output, target)) {
                int moved = Math.min(remaining, output.getMaxStackSize() - output.getCount());
                output.grow(moved);
                remaining -= moved;
            }
        }
        for (int slot = kind.outputStart(); slot < kind.outputEnd() && remaining > 0; slot++) {
            if (!items.get(slot).isEmpty()) continue;
            int moved = Math.min(remaining, target.getMaxStackSize());
            ItemStack created = target.copy();
            created.setCount(moved);
            items.set(slot, created);
            remaining -= moved;
        }
        if (remaining != 0) throw new IllegalStateException("Condenser output room changed mid-transaction");
    }

    private EmcValue valueOf(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        EmcKey key = new EmcKey(id.getNamespace(), id.getPath());
        if (level == null) return stack.getComponentsPatch().isEmpty()
            ? ProjectEX.emc().find(key).orElse(null) : null;
        // Stateful items require an explicitly exact value; never fall back and duplicate state.
        return MinecraftEmcAdapter.exactMatch(stack, level.registryAccess())
            .flatMap(ProjectEX.emc()::find).orElse(null);
    }

    private static CondenserVariant variant(ItemStack stack) {
        if (stack.isEmpty()) return new CondenserVariant("minecraft:air", null);
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String components = stack.getComponentsPatch().isEmpty() ? null : stack.getComponentsPatch().toString();
        return new CondenserVariant(id, components);
    }

    public int comparatorSignal() {
        int used = 0;
        float fullness = 0;
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                fullness += (float) stack.getCount() / stack.getMaxStackSize();
                used++;
            }
        }
        return used == 0 ? 0 : Math.min(15, 1 + (int) Math.floor(fullness / items.size() * 14));
    }

    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return canUse(player) ? new AlchemyStorageMenu(containerId, inventory, this) : null;
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        state = input.read("storage", AlchemyStorageState.CODEC).orElse(AlchemyStorageState.empty());
        ContainerHelper.loadAllItems(input, items);
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("storage", AlchemyStorageState.CODEC, state);
        ContainerHelper.saveAllItems(output, items);
    }

    @Override protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        state = components.getOrDefault(ProjectEXComponents.ALCHEMY_STORAGE_STATE, AlchemyStorageState.empty());
        components.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
    }

    @Override protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(ProjectEXComponents.ALCHEMY_STORAGE_STATE, state);
        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }

    private void changed() {
        setChanged();
        if (level != null) {
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) changed();
        return removed;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize(stack)) stack.setCount(getMaxStackSize(stack));
        changed();
    }
    @Override public boolean stillValid(Player player) { return canUse(player); }
    @Override public void clearContent() { items.clear(); changed(); }

    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        if (kind == StorageKind.ADVANCED_ALCHEMICAL_CHEST) {
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            return state.advancedConfig().allows(itemId);
        }
        if (!kind.condenser()) return true;
        if (slot == TARGET_SLOT) return valueOf(stack) != null;
        return slot < kind.inputEnd() && valueOf(stack) != null;
    }

    @Override public int[] getSlotsForFace(Direction side) {
        if (!kind.condenser()) return java.util.stream.IntStream.range(0, items.size()).toArray();
        return side.getAxis().isVertical()
            ? java.util.stream.IntStream.range(kind.outputStart(), kind.outputEnd()).toArray()
            : java.util.stream.IntStream.range(kind.inputStart(), kind.inputEnd()).toArray();
    }

    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return Arrays.stream(getSlotsForFace(side)).anyMatch(value -> value == slot) && canPlaceItem(slot, stack);
    }

    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return !kind.condenser() || (side.getAxis().isVertical() && slot >= kind.outputStart());
    }
}
