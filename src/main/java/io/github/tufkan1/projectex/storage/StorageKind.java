package io.github.tufkan1.projectex.storage;

/** Runtime layout and processing policy for ProjectEX storage blocks. */
public enum StorageKind {
    CONDENSER_MK1(42, 42, 1),
    CONDENSER_MK2(42, 42, 64),
    CONDENSER_MK3(91, 180, 512),
    ALCHEMICAL_CHEST(104, 0, 0),
    ADVANCED_ALCHEMICAL_CHEST(243, 0, 0),
    ALCHEMICAL_BAG(104, 0, 0);

    private static final int VIEW_SLOTS = 54;

    private final int inputSlots;
    private final int outputSlots;
    private final int inputBudget;

    StorageKind(int inputSlots, int outputSlots, int inputBudget) {
        this.inputSlots = inputSlots;
        this.outputSlots = outputSlots;
        this.inputBudget = inputBudget;
    }

    public int inputSlots() { return inputSlots; }
    public int outputSlots() { return outputSlots; }
    public int inputBudget() { return inputBudget; }
    public boolean condenser() { return outputSlots > 0; }
    public int inventorySlots() { return condenser() ? 1 + inputSlots + outputSlots : inputSlots; }
    public int inputStart() { return condenser() ? 1 : 0; }
    public int outputStart() { return 1 + inputSlots; }
    public int pageSize() { return VIEW_SLOTS; }
    public int inputPages() { return condenser() ? pages(inputSlots) : 0; }
    public int outputPages() { return condenser() ? pages(outputSlots) : 0; }
    public int pageCount() { return condenser() ? inputPages() + outputPages() : pages(inputSlots); }
    public boolean inputPage(int page) { return condenser() && page >= 0 && page < inputPages(); }

    /** Maps a visible menu slot without exposing the condenser target template. */
    public int storageSlot(int page, int visibleSlot) {
        if (page < 0 || page >= pageCount() || visibleSlot < 0 || visibleSlot >= VIEW_SLOTS) return -1;
        int mapped;
        if (!condenser()) {
            mapped = page * VIEW_SLOTS + visibleSlot;
            return mapped < inputSlots ? mapped : -1;
        }
        if (inputPage(page)) {
            mapped = inputStart() + page * VIEW_SLOTS + visibleSlot;
            return mapped < outputStart() ? mapped : -1;
        }
        int outputPage = page - inputPages();
        mapped = outputStart() + outputPage * VIEW_SLOTS + visibleSlot;
        return mapped < inventorySlots() ? mapped : -1;
    }

    private static int pages(int slots) { return Math.max(1, (slots + VIEW_SLOTS - 1) / VIEW_SLOTS); }
}
