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

This core intentionally does not grant knowledge by itself. Runtime items, confirmation
UI, audit events, server policy, and team adapters are consumers of this fail-closed
format and are implemented as separate server-authoritative layers.

## Preview and confirmation

The workflow issues a random confirmation token only after signature and server-policy
validation. Preview reports additions, removals, duplicates, final size, owner, mode,
and expiry without exposing a mutable player state. Pending confirmations are capped at
1,024 and expire after two minutes. Confirmation consumes the token on its first attempt,
binds it to the recipient UUID and player-state revision, revalidates the snapshot, then
consumes the snapshot replay identifier before returning an immutable merge/replace
result. The caller performs the final saved-data commit and structured audit write in
one server-thread operation.
