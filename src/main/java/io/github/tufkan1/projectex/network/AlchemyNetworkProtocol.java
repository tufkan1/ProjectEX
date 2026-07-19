package io.github.tufkan1.projectex.network;

/** Wire-level compatibility and size limits for transmutation traffic. */
public final class AlchemyNetworkProtocol {
    public static final int VERSION = 1;
    public static final int MAX_ITEM_ID_LENGTH = 256;
    public static final int MAX_BALANCE_LENGTH = 4096;

    private AlchemyNetworkProtocol() {
    }
}
