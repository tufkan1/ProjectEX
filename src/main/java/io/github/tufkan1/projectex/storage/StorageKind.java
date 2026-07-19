package io.github.tufkan1.projectex.storage;

/** Runtime layout and processing policy for ProjectEX storage blocks. */
public enum StorageKind {
    CONDENSER_MK1(42, 42, 1),
    CONDENSER_MK2(42, 42, 64),
    ALCHEMICAL_CHEST(104, 0, 0),
    ALCHEMICAL_BAG(104, 0, 0);

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
    public int pageSize() { return condenser() ? 42 : 54; }
    public int pageCount() { return 2; }
}
