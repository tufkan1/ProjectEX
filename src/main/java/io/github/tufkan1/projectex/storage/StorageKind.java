package io.github.tufkan1.projectex.storage;

/** Source-compatible inventory layouts for ProjectE and ProjectExpansion storage. */
public enum StorageKind {
    CONDENSER_MK1(91, 0, 1, true),
    CONDENSER_MK2(42, 42, 64, false),
    CONDENSER_MK3(91, 180, 512, false),
    ALCHEMICAL_CHEST(104, 0, 0, false),
    ADVANCED_ALCHEMICAL_CHEST(104, 0, 0, false),
    ALCHEMICAL_BAG(104, 0, 0, false);

    private final int inputSlots;
    private final int outputSlots;
    private final int inputBudget;
    private final boolean sharedOutput;

    StorageKind(int inputSlots, int outputSlots, int inputBudget, boolean sharedOutput) {
        this.inputSlots = inputSlots;
        this.outputSlots = outputSlots;
        this.inputBudget = inputBudget;
        this.sharedOutput = sharedOutput;
    }

    public int inputSlots() { return inputSlots; }
    public int outputSlots() { return outputSlots; }
    public int inputBudget() { return inputBudget; }
    public boolean condenser() { return inputBudget > 0; }
    public boolean sharedOutput() { return sharedOutput; }
    public int inventorySlots() { return condenser() ? 1 + inputSlots + outputSlots : inputSlots; }
    public int inputStart() { return condenser() ? 1 : 0; }
    public int inputEnd() { return inputStart() + inputSlots; }
    public int outputStart() { return sharedOutput ? inputStart() : inputEnd(); }
    public int outputEnd() { return sharedOutput ? inputEnd() : outputStart() + outputSlots; }
}
