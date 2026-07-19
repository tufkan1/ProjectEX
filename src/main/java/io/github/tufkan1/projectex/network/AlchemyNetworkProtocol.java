package io.github.tufkan1.projectex.network;

/** Wire-level compatibility and size limits for transmutation traffic. */
public final class AlchemyNetworkProtocol {
    public static final int VERSION = 1;
    public static final int MAX_ITEM_ID_LENGTH = 256;
    public static final int MAX_BALANCE_LENGTH = 4096;
    public static final int MAX_EMC_VALUE_LENGTH = 1024;
    public static final int MAX_SEARCH_LENGTH = 64;
    public static final int MAX_KNOWLEDGE_PAGE_SIZE = 54;

    private AlchemyNetworkProtocol() {
    }
}
