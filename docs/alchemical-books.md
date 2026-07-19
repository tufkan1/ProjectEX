# Alchemical Books

ProjectEX provides the ProjectExpansion four-tier destination progression:

| Tier | EMC per block | Player binding | Cross-dimension |
| --- | ---: | --- | --- |
| Basic | 1,000 | No | No |
| Advanced | 500 | Yes | No |
| Master | 100 | Yes | Yes |
| Arcane | 0 | Yes | Yes |

Normal use opens a server-authored, keyboard- and narrator-friendly destination screen.
The screen can save the current block, delete an existing name, teleport, or consume the
one-shot back target. Names are trimmed, control-free, case-insensitively unique, limited
to 32 characters, and capped at 64 destinations per store.

Unbound books persist destinations in their versioned item component. Sneak-use binds an
Advanced-or-higher book to the player's UUID and switches it to world-persistent owner
storage; only the owner can remove that binding. A bound owner must be online while the
book is used. `projectex.alchemicalBook.editPolicy` accepts `owner_only` (default),
`operator_only`, or `enabled`; non-editors may still use destinations shared by the owner.

Every action belongs to a random server-created session bound to the exact stack object
and opening hand. Payloads are size-bounded, ordered by request ID, rate limited, and
revalidated against the live component and bound-store revision. Replacing or dropping
the book, disconnecting, replaying an action, editing stale destinations, selecting a
missing dimension, crossing dimensions with a lower tier, leaving the world border, or
lacking EMC fails before teleportation.

Claim and safety mods can register `AlchemicalTeleportProtection.EVENT` and inspect the
server player, exact book stack, tier, source, and destination before EMC is charged.

EMC debit is server-side and exact. A rejected teleport is refunded; a successful
teleport records the previous position as a one-shot back destination. Destination lists,
prices, edit permission, failure status, and balance displayed by the client are all
server-authored.
