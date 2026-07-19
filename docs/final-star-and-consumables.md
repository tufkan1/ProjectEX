# Final Star and infinite consumables

## Progression

Eight Gargantuan Star Omega items around a Nether Star craft one Final Star Shard. Eight shards
around the Dragon Egg craft the Final Star. Eight cooked beef around one shard craft Infinite
Steak. These recipes are forward-only and have no decompression path.

## Capability contract

Other Fabric mods can query `FinalStarApi.LOOKUP` (API version 1) with an item stack and a
server-owned `FinalStarContext`. A returned capability reports its resolved slot and cooldown,
can be checked with `ready()`, and atomically claims the shared cooldown with `activate()`.
ProjectEX searches main hand, off hand, then ordinary inventory in deterministic order.

The server gates are JVM properties:

- `projectex.finalStar.enabled` (default `true`)
- `projectex.finalStar.slots` (default `main_hand,off_hand,inventory`)
- `projectex.finalStar.cooldownTicks` (default `20`, range 1..72000)
- `projectex.infiniteConsumables.enabled` (default `true`)
- `projectex.infiniteSteak.emcCost` (default `64`, arbitrary-precision positive integer)
- `projectex.infiniteSteak.cooldownTicks` (default `20`, range 1..72000)

Invalid settings fail startup instead of silently widening permissions.

## Server semantics

Infinite Steak never shrinks. Completion—not animation start—rechecks hunger, configuration,
payment, Final Star readiness, and cooldown on the server thread. Without a usable Final Star it
atomically debits the player's persisted EMC. With one, it atomically claims that star's shared
cooldown and charges no EMC. Hunger is changed only after one of those commits succeeds.

## Migration

This feature introduces no persisted component or saved-data schema, so existing worlds require
no migration. The new item identifiers are additive and player EMC remains in the existing
versioned player-alchemy payload.
