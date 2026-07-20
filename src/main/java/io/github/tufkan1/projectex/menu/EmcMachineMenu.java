package io.github.tufkan1.projectex.menu;

import io.github.tufkan1.projectex.content.ProjectEXMenus;
import io.github.tufkan1.projectex.content.machine.EmcMachineBlockEntity;
import io.github.tufkan1.projectex.machine.MachineTier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Exact ProjectE collector and relay slot layouts. */
public final class EmcMachineMenu extends AbstractContainerMenu {
    private final Container machine;
    private final ContainerData data;
    private final MachineTier tier;
    private int machineMenuSlots;
    private int collectorLockMenuSlot = -1;
    private final EmcMachineBlockEntity machineEntity;

    public EmcMachineMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, MachineTier.COLLECTOR_MK1);
    }

    public EmcMachineMenu(int containerId, Inventory inventory, Integer tierOrdinal) {
        this(containerId, inventory, decodeTier(tierOrdinal));
    }

    private EmcMachineMenu(int containerId, Inventory inventory, MachineTier tier) {
        this(containerId, inventory, new SimpleContainer(EmcMachineBlockEntity.machineSlots(tier)),
            fixedData(tier), tier);
    }

    public EmcMachineMenu(int containerId, Inventory inventory, EmcMachineBlockEntity machine) {
        this(containerId, inventory, machine, new ContainerData() {
            @Override public int get(int index) {
                return switch (index) {
                    case 0 -> saturatingInt(machine.machineState().stored().amount());
                    case 1 -> saturatingInt(machine.tier().capacity().amount());
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) { }
            @Override public int getCount() { return 2; }
        }, machine.tier());
    }

    private EmcMachineMenu(int containerId, Inventory inventory, Container machine,
                           ContainerData data, MachineTier tier) {
        super(ProjectEXMenus.EMC_MACHINE, containerId);
        this.machine = machine;
        this.data = data;
        this.tier = tier;
        this.machineEntity = machine instanceof EmcMachineBlockEntity entity ? entity : null;
        checkContainerSize(machine, EmcMachineBlockEntity.machineSlots(tier));
        machine.startOpen(inventory.player);
        if (relay()) addRelaySlots(inventory); else addCollectorSlots(inventory);
        addDataSlots(data);
    }

    private void addCollectorSlots(Inventory inventory) {
        int level = Math.min(3, tier.level());
        int chargeX = switch (level) { case 1 -> 124; case 2 -> 140; default -> 158; };
        addMachineSlot(EmcMachineBlockEntity.CHARGE_SLOT, chargeX, 58, true);
        int counter = 1;
        int columns = level + 1;
        int startX = level == 1 ? 20 : 18;
        for (int column = columns - 1; column >= 0; column--) {
            for (int row = 3; row >= 0; row--) {
                addMachineSlot(counter++, startX + column * 18, 8 + row * 18, true);
            }
        }
        addMachineSlot(counter++, chargeX, 13, false);
        collectorLockMenuSlot = slots.size();
        addMachineSlot(counter, chargeX + 29, 36, false);
        addStandardInventorySlots(inventory, switch (level) { case 1 -> 8; case 2 -> 20; default -> 30; }, 84);
    }

    private void addRelaySlots(Inventory inventory) {
        int level = Math.min(3, tier.level());
        int chargeX = switch (level) { case 1 -> 127; case 2 -> 144; default -> 164; };
        int burnX = switch (level) { case 1 -> 67; case 2 -> 84; default -> 104; };
        int centralY = level == 1 ? 43 : level == 2 ? 44 : 58;
        addMachineSlot(EmcMachineBlockEntity.CHARGE_SLOT, chargeX, centralY, true);
        addMachineSlot(1, burnX, centralY, true);
        int counter = 2;
        int columns = level + 1;
        int rows = level + 2;
        int startX = level == 1 ? 27 : level == 2 ? 26 : 28;
        int startY = level == 1 ? 17 : 18;
        for (int column = columns - 1; column >= 0; column--) {
            for (int row = rows - 1; row >= 0; row--) {
                addMachineSlot(counter++, startX + column * 18, startY + row * 18, true);
            }
        }
        addStandardInventorySlots(inventory,
            switch (level) { case 1 -> 8; case 2 -> 16; default -> 26; },
            switch (level) { case 1 -> 95; case 2 -> 101; default -> 113; });
    }

    private void addMachineSlot(int slot, int x, int y, boolean insert) {
        addSlot(new Slot(machine, slot, x, y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return insert && machine.canPlaceItem(getContainerSlot(), stack);
            }
        });
        machineMenuSlots++;
    }

    @Override public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (slotId == collectorLockMenuSlot && input == ContainerInput.PICKUP) {
            int lock = machineMenuSlots - 1;
            ItemStack carried = getCarried();
            if (carried.isEmpty()) {
                machine.setItem(lock, ItemStack.EMPTY);
            } else if (machineEntity == null || machineEntity.canSetCollectorTarget(carried)) {
                ItemStack template = carried.copy();
                template.setCount(1);
                machine.setItem(lock, template);
            }
            broadcastChanges();
            return;
        }
        super.clicked(slotId, button, input, player);
    }

    public int storedEmc() { return data.get(0); }
    public int capacity() { return data.get(1); }
    public MachineTier tier() { return tier; }
    private boolean relay() { return tier.type() == MachineTier.MachineType.RELAY; }

    @Override public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (slotIndex < machineMenuSlots) {
            if (!moveItemStackTo(stack, machineMenuSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, machineMenuSlots, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return original;
    }

    @Override public boolean stillValid(Player player) { return machine.stillValid(player); }
    @Override public void removed(Player player) { super.removed(player); machine.stopOpen(player); }

    private static ContainerData fixedData(MachineTier tier) {
        SimpleContainerData data = new SimpleContainerData(2);
        data.set(1, saturatingInt(tier.capacity().amount()));
        return data;
    }

    private static MachineTier decodeTier(int ordinal) {
        return ordinal >= 0 && ordinal < MachineTier.values().length
            ? MachineTier.values()[ordinal] : MachineTier.COLLECTOR_MK1;
    }

    private static int saturatingInt(java.math.BigInteger value) {
        return value.min(java.math.BigInteger.valueOf(Integer.MAX_VALUE)).intValue();
    }
}
