package io.github.tufkan1.projectex.content.machine;

import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcValue;
import io.github.tufkan1.projectex.api.storage.EmcStorage;
import io.github.tufkan1.projectex.api.storage.EmcStorageApi;
import io.github.tufkan1.projectex.api.storage.EmcStorageContext;
import io.github.tufkan1.projectex.api.storage.EmcTransferMode;
import io.github.tufkan1.projectex.content.EmcMachineBlock;
import io.github.tufkan1.projectex.content.ProjectEXBlockEntities;
import io.github.tufkan1.projectex.content.ProjectEXBlocks;
import io.github.tufkan1.projectex.content.ProjectEXItems;
import io.github.tufkan1.projectex.content.ProjectEXComponents;
import io.github.tufkan1.projectex.content.ProjectEXContentRegistry;
import io.github.tufkan1.projectex.content.component.MachineItemState;
import io.github.tufkan1.projectex.machine.FixedPointRate;
import io.github.tufkan1.projectex.machine.FuelUpgradeRule;
import io.github.tufkan1.projectex.machine.MachineBuffer;
import io.github.tufkan1.projectex.machine.MachineNetworkTick;
import io.github.tufkan1.projectex.machine.MachineState;
import io.github.tufkan1.projectex.machine.MachineStateCodec;
import io.github.tufkan1.projectex.machine.MachineTier;
import io.github.tufkan1.projectex.machine.MachineRedstoneMode;
import io.github.tufkan1.projectex.machine.MachineRuntimeConfig;
import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import io.github.tufkan1.projectex.menu.EmcMachineMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;

/** Versioned, sided and server-authoritative collector/relay block entity. */
public final class EmcMachineBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider,
    net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider<Integer> {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    private static final int[] INPUT = {INPUT_SLOT};
    private static final int[] OUTPUT = {OUTPUT_SLOT};
    private static final java.util.List<net.minecraft.world.item.Item> FUEL_PROGRESSION =
        java.util.stream.Stream.concat(
            java.util.stream.Stream.of(
                ProjectEXItems.ALCHEMICAL_COAL.item(), ProjectEXItems.MOBIUS_FUEL.item(),
                ProjectEXItems.AETERNALIS_FUEL.item()
            ),
            ProjectEXItems.EXPANSION_FUELS.stream().map(ProjectEXContentRegistry.RegisteredItem::item)
        ).toList();

    private final MachineTier tier;
    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private MachineState state;

    public EmcMachineBlockEntity(BlockPos pos, BlockState blockState) {
        super(ProjectEXBlockEntities.EMC_MACHINE, pos, blockState);
        if (!(blockState.getBlock() instanceof EmcMachineBlock machineBlock)) {
            throw new IllegalArgumentException("EMC machine block entity requires an EMC machine block");
        }
        tier = machineBlock.tier();
        state = MachineState.empty(tier);
    }

    public static void tickServer(
        net.minecraft.world.level.Level level,
        BlockPos pos,
        BlockState blockState,
        EmcMachineBlockEntity machine
    ) {
        if (!(level instanceof ServerLevel serverLevel)
            || !machine.state.redstoneMode().enabled(level.hasNeighborSignal(pos))) {
            return;
        }
        EmcValue before = machine.state.stored();
        if (machine.tier.type() == MachineTier.MachineType.COLLECTOR) {
            machine.generate();
            machine.upgradeFuel();
        } else if (machine.tier.type() == MachineTier.MachineType.RELAY) {
            machine.consumeRelayInput(serverLevel);
        } else {
            machine.generate();
        }
        machine.chargeOutput(serverLevel);
        machine.transferToNeighbors(serverLevel);
        if (!machine.state.stored().equals(before)) {
            machine.setChangedAndNotify();
        }
    }

    public MachineTier tier() {
        return tier;
    }

    @Override public Integer getScreenOpeningData(net.minecraft.server.level.ServerPlayer player) {
        return tier().ordinal();
    }

    public MachineState machineState() {
        return state;
    }

    public void claim(UUID owner) {
        state = new MachineState(
            state.version(), tier, state.stored(), state.deferredGeneration(),
            state.access().claim(owner), state.redstoneMode()
        );
        setChangedAndNotify();
    }

    public boolean canUse(Player player) {
        boolean operator = player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        return state.access().permits(player.getUUID(), operator)
            && Container.stillValidBlockEntity(this, player);
    }

    public boolean setRedstoneMode(
        MachineRedstoneMode mode,
        UUID actor,
        boolean operatorOverride
    ) {
        if (!state.access().permits(actor, operatorOverride)) {
            return false;
        }
        state = new MachineState(
            state.version(), tier, state.stored(), state.deferredGeneration(),
            state.access(), java.util.Objects.requireNonNull(mode, "mode")
        );
        setChangedAndNotify();
        return true;
    }

    public boolean setPublicAccess(boolean enabled, UUID actor, boolean operatorOverride) {
        if (!state.access().permits(actor, operatorOverride)) {
            return false;
        }
        state = new MachineState(
            state.version(), tier, state.stored(), state.deferredGeneration(),
            state.access().withPublicAccess(enabled, actor, operatorOverride), state.redstoneMode()
        );
        setChangedAndNotify();
        return true;
    }

    public int comparatorSignal() {
        return buffer().comparatorSignal();
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return canUse(player) ? new EmcMachineMenu(containerId, inventory, this) : null;
    }

    private void generate() {
        MachineBuffer buffer = buffer();
        EmcValue room = buffer.capacity().subtract(buffer.stored());
        if (room.equals(EmcValue.ZERO)) {
            return;
        }
        boolean compactSun = tier.type() == MachineTier.MachineType.POWER_FLOWER && level != null
            && level.getBlockState(worldPosition.below()).is(ProjectEXBlocks.COMPACT_SUN);
        FixedPointRate rate = MachineRuntimeConfig.generationRate(tier, compactSun);
        FixedPointRate.Generation generation = rate.generate(
            state.deferredGeneration(),
            1,
            room
        );
        buffer.insert(generation.produced());
        replaceStored(buffer.stored(), generation.deferredNumerator());
    }

    private void upgradeFuel() {
        ItemStack input = items.get(INPUT_SLOT);
        if (input.isEmpty()) {
            return;
        }
        ItemStack output = items.get(OUTPUT_SLOT);
        net.minecraft.world.item.Item resultItem = fuelUpgradeResult(input);
        if (resultItem == null) {
            return;
        }
        if (!output.isEmpty() && (!output.is(resultItem) || output.getCount() >= output.getMaxStackSize())) {
            return;
        }
        EmcValue inputValue = valueOf(input).orElse(null);
        EmcValue outputValue = valueOf(new ItemStack(resultItem)).orElse(null);
        if (inputValue == null || outputValue == null) {
            return;
        }
        MachineBuffer buffer = buffer();
        FuelUpgradeRule.Upgrade upgrade = new FuelUpgradeRule(
            BuiltInRegistries.ITEM.getKey(input.getItem()).toString(),
            BuiltInRegistries.ITEM.getKey(resultItem).toString(),
            inputValue,
            outputValue
        ).apply(BuiltInRegistries.ITEM.getKey(input.getItem()).toString(), buffer);
        if (!upgrade.upgraded()) {
            return;
        }
        input.shrink(1);
        if (output.isEmpty()) {
            items.set(OUTPUT_SLOT, new ItemStack(resultItem));
        } else {
            output.grow(1);
        }
        replaceStored(buffer.stored(), state.deferredGeneration());
    }

    private void consumeRelayInput(ServerLevel level) {
        ItemStack input = items.get(INPUT_SLOT);
        if (input.isEmpty()) {
            return;
        }
        MachineBuffer buffer = buffer();
        EmcValue room = buffer.capacity().subtract(buffer.stored())
            .min(MachineRuntimeConfig.transferLimit(tier));
        if (room.equals(EmcValue.ZERO)) {
            return;
        }
        EmcStorageApi.find(input, EmcStorageContext.automation(level)).ifPresentOrElse(storage -> {
            var extracted = storage.extract(room, EmcTransferMode.EXECUTE);
            buffer.insert(extracted.transferred());
        }, () -> valueOf(input).ifPresent(value -> {
            if (value.compareTo(room) <= 0) {
                buffer.insert(value);
                input.shrink(1);
            }
        }));
        replaceStored(buffer.stored(), state.deferredGeneration());
    }

    private void chargeOutput(ServerLevel level) {
        ItemStack output = items.get(OUTPUT_SLOT);
        if (output.isEmpty() || state.stored().equals(EmcValue.ZERO)) {
            return;
        }
        EmcStorage storage = EmcStorageApi.find(output, EmcStorageContext.automation(level)).orElse(null);
        if (storage == null) {
            return;
        }
        MachineBuffer buffer = buffer();
        var inserted = storage.insert(
            buffer.stored().min(MachineRuntimeConfig.transferLimit(tier)),
            EmcTransferMode.EXECUTE
        );
        buffer.extract(inserted.transferred());
        replaceStored(buffer.stored(), state.deferredGeneration());
    }

    private void transferToNeighbors(ServerLevel level) {
        if (state.stored().equals(EmcValue.ZERO)) {
            return;
        }
        MachineNetworkTick network = MachineNetworkCoordinator.current(level);
        for (Direction direction : Direction.values()) {
            if (state.stored().equals(EmcValue.ZERO)) {
                break;
            }
            BlockPos targetPos = worldPosition.relative(direction);
            // Machine routing must never synchronously load a neighboring chunk.
            if (!level.getChunkSource().hasChunk(targetPos.getX() >> 4, targetPos.getZ() >> 4)) {
                continue;
            }
            BlockEntity adjacent = level.getBlockEntity(targetPos);
            if (!(adjacent instanceof EmcMachineBlockEntity target)) {
                continue;
            }
            MachineBuffer sourceBuffer = buffer();
            MachineBuffer targetBuffer = target.buffer();
            MachineNetworkTick.Transfer transfer = network.route(
                nodeId(), sourceBuffer, target.nodeId(), targetBuffer,
                MachineRuntimeConfig.transferLimit(tier)
            );
            if (!transfer.moved().equals(EmcValue.ZERO)) {
                replaceStored(sourceBuffer.stored(), state.deferredGeneration());
                target.replaceStored(targetBuffer.stored(), target.state.deferredGeneration());
                target.setChangedAndNotify();
            }
        }
    }

    private java.util.Optional<EmcValue> valueOf(ItemStack stack) {
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return ProjectEX.emc().find(new EmcKey(id.getNamespace(), id.getPath()));
    }

    private MachineBuffer buffer() {
        return new MachineBuffer(tier.capacity(), state.stored());
    }

    private void replaceStored(EmcValue stored, BigInteger deferred) {
        state = new MachineState(
            state.version(), tier, stored, deferred, state.access(), state.redstoneMode()
        );
    }

    private String nodeId() {
        return worldPosition.getX() + "," + worldPosition.getY() + "," + worldPosition.getZ();
    }

    private void setChangedAndNotify() {
        setChanged();
        if (level != null) {
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        Map<String, String> encoded = new java.util.HashMap<>();
        ValueInput machine = input.childOrEmpty("machine");
        for (String key : java.util.List.of(
            "version", "tier", "stored", "deferred_generation", "owner", "public_access", "redstone_mode"
        )) {
            machine.getString(key).ifPresent(value -> encoded.put(key, value));
        }
        try {
            state = MachineStateCodec.decode(encoded, tier);
        } catch (IllegalArgumentException exception) {
            ProjectEX.LOGGER.error("Rejected corrupt {} machine state at {}", tier, worldPosition, exception);
            state = MachineState.empty(tier);
        }
        ContainerHelper.loadAllItems(input, items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ValueOutput machine = output.child("machine");
        MachineStateCodec.encode(state).forEach(machine::putString);
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        MachineItemState carried = components.get(ProjectEXComponents.MACHINE_STATE);
        if (carried != null && carried.tier() == tier) {
            state = carried.toMachineState();
        }
        components.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(ProjectEXComponents.MACHINE_STATE, MachineItemState.from(state));
        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChangedAndNotify();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize(stack)) {
            stack.setCount(getMaxStackSize(stack));
        }
        setChangedAndNotify();
    }

    @Override
    public boolean stillValid(Player player) {
        return canUse(player);
    }

    @Override
    public void clearContent() {
        items.clear();
        setChangedAndNotify();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == OUTPUT_SLOT) {
            return exposesEmcStorage(stack);
        }
        return switch (tier.type()) {
            case COLLECTOR -> fuelUpgradeResult(stack) != null;
            case RELAY -> exposesEmcStorage(stack) || valueOf(stack).isPresent();
            case POWER_FLOWER -> false;
        };
    }

    private boolean exposesEmcStorage(ItemStack stack) {
        return level instanceof ServerLevel server
            && EmcStorageApi.LOOKUP.find(stack, EmcStorageContext.automation(server)) != null;
    }

    private static net.minecraft.world.item.Item fuelUpgradeResult(ItemStack input) {
        for (int index = 0; index < FUEL_PROGRESSION.size() - 1; index++) {
            if (input.is(FUEL_PROGRESSION.get(index))) {
                return FUEL_PROGRESSION.get(index + 1);
            }
        }
        return null;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side.getAxis().isVertical() ? OUTPUT : INPUT;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return java.util.Arrays.stream(getSlotsForFace(side)).anyMatch(candidate -> candidate == slot)
            && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == OUTPUT_SLOT && side.getAxis().isVertical();
    }
}
