# Server operator guide

## Deployment checklist

Use Java 25 and a Fabric 26.2 server with Fabric Loader 0.19.3+, Fabric API
0.155.2+26.2, and the same ProjectEX jar required from clients. Verify the release checksum,
test on a staging copy, and retain relevant logs. ProjectEX does not chunk-load and is audited
for 128 concurrent player records and 1,024 active machines per level; this is a supported
test envelope, not a hardware guarantee.

On first start ProjectEX writes commented schema-v1 files under `config/projectex/`.
Review common and server policy while stopped. Client settings never grant authority.
Unknown, missing, unsupported, or out-of-range values fail instead of silently defaulting.

## Backups and upgrades

1. Announce maintenance, stop automation, run `/save-all flush`, then stop the server.
2. Copy the complete world and `config/projectex/` to versioned, access-controlled storage.
3. On staging, run `/projectex migration dry-run`, then `/projectex migration backup`.
4. Install the new jar and start staging. Run `/projectex status`, `/projectex config status`,
   `/projectex datapack audit`, and a representative machine/player journey.
5. Repeat on production only after success. Retain the pre-upgrade backup until a clean save,
   shutdown, restart, and gameplay verification complete.

ProjectEX auto-applies supported required migrations at server start and refuses data newer
than the running mod. Exact roots and schemas are in
[configuration and migrations](configuration-and-migrations.md).

## Operations and diagnostics

`/projectex status` and `/projectex emc <item>` are safe queries. Operator commands include
`reload`, `dump`, `config status|reload`, `datapack audit`, `machine audit <x y z>`,
`migration status|report|dry-run|backup|apply|recovery`, and player inspect/reset/recovery.
Treat dumps and player UUID output as administrative data. Reset is destructive and should
follow a verified backup and operator record.

Use `/projectex config reload` for common/server settings and Minecraft `/reload` for data
packs. Both publish atomically: a failure retains the last valid snapshot. Monitor rate-limit,
denied-action, migration, and recovery events without exposing signed payloads or secrets.

## Restore and incident response

Never restore while the server runs. Use `/projectex migration recovery <backup-id>` to
prepare an offline package and `RESTORE.txt`, stop the server, verify paths and hashes, and
copy only documented roots. Start without players, inspect migration/config/datapack status,
and retain the failed world for investigation.

For corrupt player data, export the preserved payload before clearing it. For a suspected
vulnerability follow [SECURITY.md](../SECURITY.md), not a public issue. For ordinary failures
use the [player troubleshooting checklist](player-guide.md#troubleshooting).
