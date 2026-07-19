# Configuration, upgrades, and operator recovery

ProjectEX 1.0 freezes configuration and persisted-data format `1`. Configuration lives
under `config/projectex/` and is split by ownership:

- `common.properties`: rules meaningful in integrated and dedicated play.
- `server.properties`: authoritative gameplay, rate, budget, sharing, and access policy.
- `client.properties`: local preferences only; it cannot change authoritative values.

Each file declares `schema_version=1` and documents every setting inline. Unknown keys,
missing keys, unsupported schemas, and invalid values stop loading with the absolute file
path and exact key. ProjectEX never replaces an invalid file with defaults. `-D` JVM
properties remain explicit boot-time overrides; file reloads do not overwrite them.

## Server settings

| Area | Keys | Safe bounds/default |
|---|---|---|
| Machine budget | `projectex.machine.maxTransfersPerWorldTick`, `projectex.machine.maxEmcPerWorldTick` | `65536`, `2^256`; positive and bounded |
| Machine balance | `compactSunMultiplier`, collector/relay/power-flower rate multipliers | `10`, `1`, `1`, `1`; positive decimal or fraction |
| Final Star | `projectex.finalStar.enabled`, `.slots`, `.cooldownTicks` | enabled; supported slots; 20 ticks |
| Infinite items | `projectex.infiniteConsumables.enabled`, `projectex.infiniteSteak.*` | enabled; 64 EMC; 20 ticks |
| Knowledge | tome mode, sharing policy, signed snapshot lifetime | consume; enabled; 24 hours |
| Alchemical Books | `projectex.alchemicalBook.editPolicy` | `owner_only` |
| Destructive actions | `projectex.destructiveCatalysts.enabled` | `true` |

Run `/projectex config status` to inspect the loaded schema and `/projectex config reload`
to validate and atomically publish common/server changes. A failed reload keeps every
previous runtime setting; it never exposes a partially updated configuration.

## Upgrade workflow

On the first start of an older world, the server performs the same idempotent apply before
normal play: it refuses newer formats and publishes the baseline backup before the first
ProjectEX autosave. Operators can rehearse and repeat the workflow explicitly:

1. Run `/projectex migration dry-run`. It lists only bounded ProjectEX world-data and
   configuration candidates and computes their SHA-256 hashes without writing anything.
2. Run `/projectex migration apply`. ProjectEX first publishes a complete backup with a
   versioned JSON manifest, then atomically advances `world/projectex/migration.properties`.
3. Keep `world/projectex/backups/<backup-id>` until the upgraded server has saved and
   restarted cleanly. `/projectex migration status` reports the active format and backup.

`/projectex migration backup` creates the same manifest-backed snapshot without changing
the format marker; `/projectex migration report` is an alias for the status report.

The first public alpha (`0.1.0-alpha.1`) is the format-0 baseline. Its player schema v0
is upgraded from `emc` to `balance`; current component/block/preference formats remain
version 1. Committed fixtures under `src/test/resources/fixtures/0.1.0-alpha.1/` are
decoded on every build. A marker newer than the running mod is rejected.

## Recovery and rollback

ProjectEX does not overwrite live world files from a command. Use
`/projectex migration recovery <backup-id>` to create a bounded offline recovery package
with `RESTORE.txt`, then stop the server before copying it into the documented roots.
This prevents the in-memory server from overwriting restored data during shutdown.

If corrupt player alchemy data entered recovery mode:

- `/projectex player recovery status` reports it without exposing user data.
- `/projectex player recovery export` writes the exact payload atomically under
  `world/projectex/recovery/`.
- After verifying the export, `/projectex player recovery clear` removes the preserved
  in-world copy and records an operator audit log.

Additional diagnostics are `/projectex datapack audit`, `/projectex emc <item>`,
`/projectex dump`, and `/projectex machine audit <x y z>`. EMC conflicts are fatal during
reload, so the previous snapshot remains active. The audit retains a bounded last-failure
message with resource/candidate counts, while every winning value keeps source provenance.
