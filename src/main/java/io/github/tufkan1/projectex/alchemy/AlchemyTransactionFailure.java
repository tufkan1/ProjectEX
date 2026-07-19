package io.github.tufkan1.projectex.alchemy;

/** Stable rejection reasons suitable for networking, audit logs, and UI messages. */
public enum AlchemyTransactionFailure {
    NONE,
    SESSION_INVALID,
    TOO_FAR,
    RATE_LIMITED,
    STATE_CHANGED,
    STALE_EMC_REVISION,
    INVALID_COUNT,
    UNKNOWN_EMC_VALUE,
    ITEM_NOT_PRESENT,
    ITEM_NOT_LEARNED,
    INSUFFICIENT_EMC,
    INVENTORY_FULL,
    BALANCE_LIMIT_EXCEEDED
}
