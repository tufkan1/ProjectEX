package io.github.tufkan1.projectex.menu;

import io.github.tufkan1.projectex.content.ProjectEXMenus;
import io.github.tufkan1.projectex.content.storage.AlchemyStorageBlockEntity;
import io.github.tufkan1.projectex.storage.StorageKind;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Native ProjectE/ProjectExpansion storage layouts without synthetic pagination. */
public final class AlchemyStorageMenu extends AbstractContainerMenu {
    private final Container storage;
    private final StorageKind kind;
    private final boolean outputView;
    private int storageMenuSlots;
    private int targetMenuSlot = -1;

    public AlchemyStorageMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(StorageKind.ALCHEMICAL_CHEST.inventorySlots()),
            StorageKind.ALCHEMICAL_CHEST, false);
    }

    public AlchemyStorageMenu(int containerId, Inventory inventory, Integer openingData) {
        this(containerId, inventory, new SimpleContainer(decodeKind(openingData).inventorySlots()),
            decodeKind(openingData), decodeOutputView(openingData));
    }

    public AlchemyStorageMenu(int containerId, Inventory inventory, AlchemyStorageBlockEntity storage) {
        this(containerId, inventory, storage, storage.kind(), false);
    }

    public AlchemyStorageMenu(int containerId, Inventory inventory,
                              AlchemyStorageBlockEntity storage, boolean outputView) {
        this(containerId, inventory, storage, storage.kind(), outputView);
    }

    public AlchemyStorageMenu(int containerId, Inventory inventory, Container storage, StorageKind kind) {
        this(containerId, inventory, storage, kind, false);
    }

    private AlchemyStorageMenu(int containerId, Inventory inventory, Container storage,
                               StorageKind kind, boolean requestedOutputView) {
        super(ProjectEXMenus.ALCHEMY_STORAGE, containerId);
        this.storage = storage;
        this.kind = kind;
        this.outputView = kind == StorageKind.CONDENSER_MK3 && requestedOutputView;
        checkContainerSize(storage, kind.inventorySlots());
        storage.startOpen(inventory.player);

        if (!kind.condenser()) {
            addGrid(kind.inputStart(), 13, 8, 12, 5, true);
            addStandardInventorySlots(inventory, 48, 152);
        } else if (kind == StorageKind.CONDENSER_MK1) {
            addTargetSlot(12, 6);
            addGrid(kind.inputStart(), 13, 7, 12, 26, true);
            addStandardInventorySlots(inventory, 48, 154);
        } else if (kind == StorageKind.CONDENSER_MK2) {
            addTargetSlot(12, 6);
            addGrid(kind.inputStart(), 6, 7, 12, 26, true);
            addGrid(kind.outputStart(), 6, 7, 138, 26, false);
            addStandardInventorySlots(inventory, 48, 154);
        } else if (outputView) {
            addGrid(kind.outputStart(), 20, 9, 12, 8, false);
            addStandardInventorySlots(inventory, 111, 172);
        } else {
            addTargetSlot(12, 6);
            addGrid(kind.inputStart(), 13, 7, 12, 26, true);
            addStandardInventorySlots(inventory, 48, 154);
        }
    }

    public static int openingData(StorageKind kind, boolean outputView) {
        return (kind.ordinal() << 1) | (outputView ? 1 : 0);
    }

    public StorageKind kind() { return kind; }
    public boolean outputView() { return outputView; }

    private void addGrid(int firstSlot, int columns, int rows, int x, int y, boolean allowInsertion) {
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                addStorageSlot(firstSlot + column + row * columns,
                    x + column * 18, y + row * 18, allowInsertion);
            }
        }
    }

    private void addStorageSlot(int storageSlot, int x, int y, boolean allowInsertion) {
        addSlot(new Slot(storage, storageSlot, x, y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return allowInsertion && storage.canPlaceItem(getContainerSlot(), stack);
            }
        });
        storageMenuSlots++;
    }

    private void addTargetSlot(int x, int y) {
        targetMenuSlot = slots.size();
        addStorageSlot(AlchemyStorageBlockEntity.TARGET_SLOT, x, y, false);
    }

    @Override public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (slotId == targetMenuSlot && input == ContainerInput.PICKUP) {
            ItemStack carried = getCarried();
            if (carried.isEmpty()) {
                storage.setItem(AlchemyStorageBlockEntity.TARGET_SLOT, ItemStack.EMPTY);
            } else if (storage.canPlaceItem(AlchemyStorageBlockEntity.TARGET_SLOT, carried)) {
                ItemStack template = carried.copy();
                template.setCount(1);
                storage.setItem(AlchemyStorageBlockEntity.TARGET_SLOT, template);
            }
            broadcastChanges();
            return;
        }
        super.clicked(slotId, button, input, player);
    }

    @Override public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (slotIndex < storageMenuSlots) {
            if (!moveItemStackTo(stack, storageMenuSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, storageMenuSlots, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override public boolean stillValid(Player player) { return storage.stillValid(player); }
    @Override public void removed(Player player) { super.removed(player); storage.stopOpen(player); }

    private static StorageKind decodeKind(int openingData) {
        int ordinal = openingData >>> 1;
        return ordinal >= 0 && ordinal < StorageKind.values().length
            ? StorageKind.values()[ordinal] : StorageKind.ALCHEMICAL_CHEST;
    }

    private static boolean decodeOutputView(int openingData) { return (openingData & 1) != 0; }
}
