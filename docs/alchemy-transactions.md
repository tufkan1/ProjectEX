# Alchemy transaction contract

Learn, burn, and create are evaluated on the logical server from four immutable
inputs: player state, a counted inventory snapshot, client intent, and one EMC
snapshot revision. The evaluator returns complete replacement state or the exact
unchanged inputs on rejection. Callers commit a successful pair together.

Client intent contains only an item match, count, and the revision the UI observed.
It never contains an EMC amount. The server resolves the unit value from its current
snapshot and rejects stale revisions, zero/unknown values, invalid counts, absent
items, unknown knowledge, insufficient balance, full inventory, and balance limits.

`Learn` requires possession and does not consume the item. `Burn` consumes items,
credits their server-resolved value, and learns the item identity. `Create` requires
knowledge, debits first in the proposed immutable state, and succeeds only when the
complete output fits. Requests are limited to 64 items.

Before evaluation, `AlchemyRequestGuard` verifies server-computed connection/menu
state, a maximum squared distance of 64, and a per-player sliding limit of 20 requests
per second. Disconnect clears the player's limiter state. Networking must construct
this context from server facts rather than payload fields.

`ServerAlchemyTransactionExecutor` finishes the boundary with one target-owned
compare-and-commit call. If player state or inventory changed after evaluation, the
target returns false and the executor reports `STATE_CHANGED`. Every attempt emits a
structured amount-free audit event; audit sink failures are logged and isolated from
game state.

Component-bearing item variants require an exact `EmcMatch`; componentless values are
not silently used for arbitrary custom components. Knowledge is intentionally tracked
by item identifier, while the exact match controls valuation and inventory identity.
