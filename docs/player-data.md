# Player alchemy data

ProjectEX stores one server-wide, UUID-keyed alchemy record in the overworld saved-data
storage. The record contains an arbitrary-precision non-negative EMC balance (limited
to 4096 decimal digits for corruption/denial-of-service safety) and a sorted set of
learned item identifiers.

The current payload schema is version 1. Version 0 payloads using the `emc` field are
migrated to the version 1 `balance` field on the next save. Unknown fields, negative
balances, invalid UUIDs/item identifiers, and unsupported versions are rejected.

## Lifecycle policy

- Death and respawn preserve state because identity is the player's UUID.
- Dimension transfer preserves state; data is server-wide rather than dimension-local.
- Logout and server restart preserve state through Minecraft `SavedData`.
- Changing a player's UUID intentionally creates a separate identity; administrators
  can inspect/reset the old UUID with `/projectex player` commands.

## Corruption recovery

If the domain payload cannot be decoded, ProjectEX starts with an empty in-memory map
and preserves the exact raw payload plus the decode error inside the saved-data record.
The server log records the failure. Before making changes, stop the server and copy
`world/data/projectex_player_alchemy.dat` to a safe location.

- `/projectex player recovery` reports whether a preserved recovery payload exists.
- `/projectex player inspect <uuid>` reports balance and learned-item count.
- `/projectex player reset <uuid>` removes one record and writes an operator audit log.

The raw recovery backup is intentionally not printed to chat or logs because future
schemas may contain user-controlled data. Backup removal/import tooling will require
an explicit migration command before the first stable release.
