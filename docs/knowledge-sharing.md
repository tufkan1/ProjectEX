# Knowledge sharing security contract

## Arcane Tablet

The Arcane Tablet carries a versioned transmutation/crafting mode. Transmutation opens
the existing M2 menu, session registry, payload validation, paging, search, and favorites
without a parallel protocol. Crafting opens the vanilla server-owned 3×3 menu. Both
portable menus bind authorization to the exact opening stack object and become invalid
when that stack leaves or is replaced in the opening hand. Sneak-use cycles modes with
localized overlay feedback.

Knowledge-sharing items use a canonical version-1 snapshot containing a random snapshot
identifier, owner UUID, issue/expiry timestamps, and at most 4,096 sorted unique EMC keys.
The server signs the canonical bytes with HMAC-SHA256; the signing secret is never sent
to clients or stored on the item.

The verification boundary rejects altered signatures, timestamps more than 60 seconds in
the future, expired snapshots, lifetimes over seven days, unsupported versions, and
oversized entry lists before any player state is considered. Confirmed snapshot UUIDs go
through a bounded one-shot replay registry. Preview and confirmation workflows must call
verification again immediately before their atomic merge/replace commit.

The runtime stores its random 256-bit signing secret and replay ledger in world saved
data. The secret never enters an item component or client payload. Every capture,
preview, denial, cancellation, confirmation, and disconnect is written to a bounded
world-persistent audit ring and the server log.

## Preview and confirmation

The workflow issues a random confirmation token only after signature and server-policy
validation. Preview reports additions, removals, duplicates, final size, owner, mode,
and expiry without exposing a mutable player state. Pending confirmations are capped at
1,024 and expire after two minutes. Confirmation consumes the token on its first attempt,
binds it to the recipient UUID and player-state revision, revalidates the snapshot, then
consumes the snapshot replay identifier before returning an immutable merge/replace
result. The runtime performs the final revision-checked saved-data commit and audit write
on the server thread. Confirmation additionally requires the same signed book to remain
in either hand; dropping or replacing it invalidates the pending operation.

## Runtime controls and integrations

Sneak-use stores or refreshes the owner's signed snapshot. Normal use by the owner cycles
merge/replace mode; normal use by another player opens the narrated vanilla confirmation
screen. Closing or rejecting the screen sends a denial and never changes knowledge.

`projectex.knowledgeSharing.policy` accepts `enabled`, `creative_only`, or `disabled`.
`projectex.knowledgeSharing.lifetimeHours` accepts 1–168 and defaults to 24. Team and
claim integrations can register a `KnowledgeSharingBoundary`; denial happens before a
confirmation token is issued.
