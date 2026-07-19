package io.github.tufkan1.projectex.menu;

import io.github.tufkan1.projectex.content.ProjectEXMenus;
import io.github.tufkan1.projectex.content.matter.MatterFurnaceBlockEntity;
import io.github.tufkan1.projectex.matter.MatterTier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Accessible furnace menu with two inputs, eighteen output positions, and synced progress. */
public final class MatterFurnaceMenu extends AbstractContainerMenu {
    private static final int FURNACE_SLOTS = 20;
    private final Container furnace;
    private final ContainerData data;

    public MatterFurnaceMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(FURNACE_SLOTS), new SimpleContainerData(4));
    }

    public MatterFurnaceMenu(int id, Inventory inventory, MatterFurnaceBlockEntity furnace) {
        this(id, inventory, furnace, new ContainerData() {
            @Override public int get(int index) {
                return switch (index) {
                    case 0 -> furnace.tier().id().equals(MatterTier.RED.id()) ? 1 : 0;
                    case 1 -> furnace.burnRemaining();
                    case 2 -> furnace.burnTotal();
                    case 3 -> furnace.cookProgress();
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) { }
            @Override public int getCount() { return 4; }
        });
    }

    private MatterFurnaceMenu(int id, Inventory inventory, Container furnace, ContainerData data) {
        super(ProjectEXMenus.MATTER_FURNACE, id);
        this.furnace = furnace;
        this.data = data;
        checkContainerSize(furnace, FURNACE_SLOTS);
        checkContainerDataCount(data, 4);
        furnace.startOpen(inventory.player);
        addSlot(inputSlot(furnace, MatterFurnaceBlockEntity.INPUT_SLOT, 17, 28));
        addSlot(inputSlot(furnace, MatterFurnaceBlockEntity.FUEL_SLOT, 17, 55));
        for (int row = 0; row < 2; row++) for (int column = 0; column < 9; column++) {
            int slot = MatterFurnaceBlockEntity.OUTPUT_START + row * 9 + column;
            addSlot(new Slot(furnace, slot, 44 + column * 18, 28 + row * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        }
        addStandardInventorySlots(inventory, 26, 106);
        addDataSlots(data);
    }

    private static Slot inputSlot(Container container, int slot, int x, int y) {
        return new Slot(container, slot, x, y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return container.canPlaceItem(getContainerSlot(), stack);
            }
        };
    }

    public MatterTier tier() { return data.get(0) == 1 ? MatterTier.RED : MatterTier.DARK; }
    public int litPixels(int width) {
        return data.get(2) <= 0 ? 0 : Math.min(width, data.get(1) * width / data.get(2));
    }
    public int cookPixels(int width) {
        return Math.min(width, data.get(3) * width / tier().furnaceCookTicks());
    }

    @Override public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (slotIndex < FURNACE_SLOTS) {
            if (!moveItemStackTo(stack, FURNACE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (slots.get(MatterFurnaceBlockEntity.FUEL_SLOT).mayPlace(stack)) {
            if (!moveItemStackTo(stack, MatterFurnaceBlockEntity.FUEL_SLOT,
                    MatterFurnaceBlockEntity.FUEL_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, MatterFurnaceBlockEntity.INPUT_SLOT,
                MatterFurnaceBlockEntity.INPUT_SLOT + 1, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return original;
    }

    @Override public boolean stillValid(Player player) { return furnace.stillValid(player); }
    @Override public void removed(Player player) {
        super.removed(player);
        furnace.stopOpen(player);
    }
}
