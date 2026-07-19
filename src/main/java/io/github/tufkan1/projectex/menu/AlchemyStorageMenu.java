package io.github.tufkan1.projectex.menu;

import io.github.tufkan1.projectex.content.ProjectEXMenus;
import io.github.tufkan1.projectex.content.storage.AlchemyStorageBlockEntity;
import io.github.tufkan1.projectex.storage.StorageKind;
import java.util.function.IntSupplier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Server-paged menu for bounded condenser and alchemical storage layouts. */
public final class AlchemyStorageMenu extends AbstractContainerMenu {
    private static final int VIEW_SLOTS = 54;
    private static final int STORAGE_MENU_SLOTS = 1 + VIEW_SLOTS;

    private final Container storage;
    private final ContainerData data;
    private final AlchemyStorageBlockEntity storageEntity;
    private final PageContainer page;

    public AlchemyStorageMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(105), new SimpleContainerData(3), null);
    }

    public AlchemyStorageMenu(
        int containerId, Inventory inventory, AlchemyStorageBlockEntity storage
    ) {
        this(containerId, inventory, storage, new ContainerData() {
            private int page;
            @Override public int get(int index) {
                return switch (index) {
                    case 0 -> storage.kind().ordinal();
                    case 1 -> page;
                    case 2 -> storage.storageState().access().publicAccess() ? 1 : 0;
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) { if (index == 1) page = value; }
            @Override public int getCount() { return 3; }
        }, storage);
    }

    public AlchemyStorageMenu(
        int containerId, Inventory inventory, Container storage, StorageKind kind
    ) {
        this(containerId, inventory, storage, fixedData(kind), null);
    }

    private static ContainerData fixedData(StorageKind kind) {
        return new ContainerData() {
            private int page;
            @Override public int get(int index) {
                return switch (index) { case 0 -> kind.ordinal(); case 1 -> page; default -> 0; };
            }
            @Override public void set(int index, int value) { if (index == 1) page = value; }
            @Override public int getCount() { return 3; }
        };
    }

    private AlchemyStorageMenu(
        int containerId, Inventory inventory, Container storage, ContainerData data,
        AlchemyStorageBlockEntity storageEntity
    ) {
        super(ProjectEXMenus.ALCHEMY_STORAGE, containerId);
        this.storage = storage;
        this.data = data;
        this.storageEntity = storageEntity;
        storage.startOpen(inventory.player);
        IntSupplier kindOrdinal = () -> data.get(0);
        boolean directClientView = storageEntity == null && storage.getContainerSize() == 105;
        page = new PageContainer(storage, kindOrdinal, () -> data.get(1), directClientView);
        Container target = new TargetContainer(storage, kindOrdinal, directClientView);

        addSlot(new Slot(target, 0, 8, 18) {
            @Override public boolean mayPlace(ItemStack stack) {
                return target.canPlaceItem(0, stack);
            }
        });
        for (int row = 0; row < 6; row++) {
            for (int column = 0; column < 9; column++) {
                int index = column + row * 9;
                addSlot(new Slot(page, index, 8 + column * 18, 40 + row * 18) {
                    @Override public boolean mayPlace(ItemStack stack) {
                        return page.canPlaceItem(getContainerSlot(), stack);
                    }
                });
            }
        }
        addStandardInventorySlots(inventory, 8, 166);
        addDataSlots(data);
    }

    public StorageKind kind() {
        int ordinal = data.get(0);
        return ordinal >= 0 && ordinal < StorageKind.values().length
            ? StorageKind.values()[ordinal] : StorageKind.ALCHEMICAL_CHEST;
    }
    public int page() { return data.get(1); }
    public boolean publicAccess() { return data.get(2) != 0; }
    public int pageCount() { return kind().pageCount(); }
    public boolean inputPage() { return kind().inputPage(page()); }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id < kind().pageCount()) {
            if (id >= kind().pageCount()) return false;
            data.set(1, id);
            broadcastChanges();
            return true;
        }
        if (id == 100 || id == 101) {
            int next = Math.floorMod(page() + (id == 100 ? -1 : 1), kind().pageCount());
            data.set(1, next);
            broadcastChanges();
            return true;
        }
        return (id == 102 || (id == 2 && kind().pageCount() == 2))
            && storageEntity != null && storageEntity.canUse(player)
            && storageEntity.setPublicAccess(!storageEntity.storageState().access().publicAccess(), player);
    }

    @Override public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (slotIndex < STORAGE_MENU_SLOTS) {
            if (!moveItemStackTo(stack, STORAGE_MENU_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            int targetEnd = kind().condenser() ? STORAGE_MENU_SLOTS : STORAGE_MENU_SLOTS;
            if (!moveItemStackTo(stack, 0, targetEnd, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override public boolean stillValid(Player player) { return storage.stillValid(player); }
    @Override public void removed(Player player) { super.removed(player); storage.stopOpen(player); }

    private static StorageKind decode(IntSupplier ordinal) {
        int value = ordinal.getAsInt();
        return value >= 0 && value < StorageKind.values().length
            ? StorageKind.values()[value] : StorageKind.ALCHEMICAL_CHEST;
    }

    private static final class TargetContainer extends DelegatingContainer {
        private final IntSupplier kind;
        private final boolean directClientView;
        private TargetContainer(Container backing, IntSupplier kind, boolean directClientView) {
            super(backing); this.kind = kind; this.directClientView = directClientView;
        }
        @Override protected int map(int slot) {
            return directClientView || decode(kind).condenser() ? 0 : -1;
        }
        @Override public int getContainerSize() { return 1; }
    }

    private static final class PageContainer extends DelegatingContainer {
        private final IntSupplier kind;
        private final IntSupplier page;
        private final boolean directClientView;
        private PageContainer(
            Container backing, IntSupplier kind, IntSupplier page, boolean directClientView
        ) {
            super(backing); this.kind = kind; this.page = page; this.directClientView = directClientView;
        }
        @Override protected int map(int slot) {
            if (directClientView) return slot + 1;
            StorageKind layout = decode(kind);
            int mapped = layout.storageSlot(page.getAsInt(), slot);
            return mapped >= 0 && mapped < backing.getContainerSize() ? mapped : -1;
        }
        @Override public int getContainerSize() { return VIEW_SLOTS; }
    }

    private abstract static class DelegatingContainer implements Container {
        protected final Container backing;
        private DelegatingContainer(Container backing) { this.backing = backing; }
        protected abstract int map(int slot);
        @Override public boolean isEmpty() {
            for (int slot = 0; slot < getContainerSize(); slot++) if (!getItem(slot).isEmpty()) return false;
            return true;
        }
        @Override public ItemStack getItem(int slot) {
            int mapped = map(slot); return mapped < 0 ? ItemStack.EMPTY : backing.getItem(mapped);
        }
        @Override public ItemStack removeItem(int slot, int amount) {
            int mapped = map(slot); return mapped < 0 ? ItemStack.EMPTY : backing.removeItem(mapped, amount);
        }
        @Override public ItemStack removeItemNoUpdate(int slot) {
            int mapped = map(slot); return mapped < 0 ? ItemStack.EMPTY : backing.removeItemNoUpdate(mapped);
        }
        @Override public void setItem(int slot, ItemStack stack) {
            int mapped = map(slot); if (mapped >= 0) backing.setItem(mapped, stack);
        }
        @Override public void setChanged() { backing.setChanged(); }
        @Override public boolean stillValid(Player player) { return backing.stillValid(player); }
        @Override public void clearContent() {
            for (int slot = 0; slot < getContainerSize(); slot++) setItem(slot, ItemStack.EMPTY);
        }
        @Override public boolean canPlaceItem(int slot, ItemStack stack) {
            int mapped = map(slot); return mapped >= 0 && backing.canPlaceItem(mapped, stack);
        }
    }
}
