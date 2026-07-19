package io.github.tufkan1.projectex.menu;

import io.github.tufkan1.projectex.content.ProjectEXMenus;
import io.github.tufkan1.projectex.content.machine.EmcMachineBlockEntity;
import io.github.tufkan1.projectex.machine.MachineTier;
import io.github.tufkan1.projectex.machine.MachineRedstoneMode;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Two-slot collector/relay menu synchronized from authoritative block-entity data. */
public final class EmcMachineMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = 2;
    private final Container machine;
    private final ContainerData data;
    private final EmcMachineBlockEntity machineEntity;

    public EmcMachineMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(MACHINE_SLOTS), new SimpleContainerData(5), null);
    }

    public EmcMachineMenu(
        int containerId,
        Inventory inventory,
        EmcMachineBlockEntity machine
    ) {
        this(containerId, inventory, machine, new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> saturatingInt(machine.machineState().stored().amount());
                    case 1 -> saturatingInt(machine.tier().capacity().amount());
                    case 2 -> machine.tier().ordinal();
                    case 3 -> machine.machineState().redstoneMode().ordinal();
                    case 4 -> machine.machineState().access().publicAccess() ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // Client data slots are read-only.
            }

            @Override
            public int getCount() {
                return 5;
            }

            private int saturatingInt(java.math.BigInteger value) {
                return value.min(java.math.BigInteger.valueOf(Integer.MAX_VALUE)).intValue();
            }
        }, machine);
    }

    private EmcMachineMenu(
        int containerId,
        Inventory inventory,
        Container machine,
        ContainerData data,
        EmcMachineBlockEntity machineEntity
    ) {
        super(ProjectEXMenus.EMC_MACHINE, containerId);
        this.machine = machine;
        this.data = data;
        this.machineEntity = machineEntity;
        checkContainerSize(machine, MACHINE_SLOTS);
        checkContainerDataCount(data, 5);
        machine.startOpen(inventory.player);
        addSlot(new Slot(machine, EmcMachineBlockEntity.INPUT_SLOT, 53, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return machine.canPlaceItem(getContainerSlot(), stack);
            }
        });
        addSlot(new Slot(machine, EmcMachineBlockEntity.OUTPUT_SLOT, 107, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return machine.canPlaceItem(getContainerSlot(), stack);
            }
        });
        addStandardInventorySlots(inventory, 8, 84);
        addDataSlots(data);
    }

    public int storedEmc() {
        return data.get(0);
    }

    public int capacity() {
        return data.get(1);
    }

    public MachineTier tier() {
        int ordinal = data.get(2);
        return ordinal >= 0 && ordinal < MachineTier.values().length
            ? MachineTier.values()[ordinal]
            : MachineTier.COLLECTOR_MK1;
    }

    public MachineRedstoneMode redstoneMode() {
        int ordinal = data.get(3);
        return ordinal >= 0 && ordinal < MachineRedstoneMode.values().length
            ? MachineRedstoneMode.values()[ordinal]
            : MachineRedstoneMode.IGNORED;
    }

    public boolean publicAccess() {
        return data.get(4) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (machineEntity == null || !machineEntity.canUse(player)) {
            return false;
        }
        boolean operator = player.permissions().hasPermission(
            net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER
        );
        if (id == 0) {
            MachineRedstoneMode[] modes = MachineRedstoneMode.values();
            MachineRedstoneMode next = modes[(machineEntity.machineState().redstoneMode().ordinal() + 1)
                % modes.length];
            return machineEntity.setRedstoneMode(next, player.getUUID(), operator);
        }
        if (id == 1) {
            return machineEntity.setPublicAccess(
                !machineEntity.machineState().access().publicAccess(),
                player.getUUID(),
                operator
            );
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (slotIndex < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        machine.stopOpen(player);
    }
}
