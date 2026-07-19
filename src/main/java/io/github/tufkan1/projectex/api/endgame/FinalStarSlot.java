package io.github.tufkan1.projectex.api.endgame;

/** Player inventory locations that a Final Star policy may authorize. */
public enum FinalStarSlot {
    MAIN_HAND("main_hand"), OFF_HAND("off_hand"), INVENTORY("inventory");

    private final String serializedName;
    FinalStarSlot(String serializedName) { this.serializedName = serializedName; }
    public String serializedName() { return serializedName; }
}
