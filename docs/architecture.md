# Architecture

## Principles

1. The server owns EMC, knowledge, inventories, machines, and transactions.
2. Domain logic is loader-neutral where practical and unit-testable without Minecraft.
3. Data packs and tags define pack policy; Java defines invariants and execution.
4. Reloads publish immutable snapshots atomically.
5. Persistent formats are versioned before the first public world is created.
6. Optional integrations live behind entrypoints and never become hard dependencies.

## Planned packages

| Package | Responsibility |
| --- | --- |
| `api.emc` | Public identifiers, values, queries, and immutable views |
| `api.fabric` | Supported Minecraft/Fabric adapters for public domain types |
| `internal.emc` | Mutable publication state; never a consumer dependency |
| `emc.data` | JSON schema, reload, validation, provenance, and reports |
| `emc.mapping` | Recipe graph evaluation and exploit-resistant calculation |
| `alchemy` | Atomic learn/burn/create domain transactions |
| `player` | Versioned persistent balance and knowledge |
| `content` | Blocks, items, components, menus, and block entities |
| `network` | Versioned, validated server/client payloads |
| `client` | Screens, rendering, keybinds, and client-only integration |
| `compat` | Optional EMI/Jade/WTHIT-style adapters |
| `datagen` | Recipes, tags, models, loot, advancements, and language output |

## EMC representation

EMC uses non-negative `BigInteger` values. Machine rates may later use a separate
rational/fixed-point type; decimals must never silently enter item valuation. The
internal registry stages data-pack results, adds recipe-derived values, and then
publishes one sorted immutable snapshot under a write lock. Readers and reload
listeners cannot observe the intermediate pre-recipe generation.

## Transaction boundary

All state-changing actions become command objects executed on the logical server.
A transaction validates the current menu/session, resolves item identity and EMC
from server state, calculates the complete result, then applies inventory and EMC
changes as one operation. Failures return a typed result and change nothing.
The loader-neutral evaluator accepts immutable snapshots; Minecraft menu/inventory
adapters are responsible for committing the returned inventory/state pair together.

## Recipe mapping invariants

The normalized recipe mapper treats explicit values as immutable roots, chooses the
lowest known tag alternative deterministically, accounts for counts and returned
containers, and rounds per-output costs upward. Derived dependency closures prevent
direct or indirect feedback recipes from lowering their own value. Zero/negative net
cost equations are ignored. Minecraft recipe serializers are adapted into this model
outside the mapping package so unsupported recipe types can be excluded explicitly.

## Compatibility boundary

Minecraft-facing adapters convert `Identifier`, registry entries, recipes, and item
stacks into domain inputs. This boundary reduces forward-port surface area. Mixins
require an ADR explaining why a Fabric event/API or composition cannot solve the
problem and must include a focused regression test.

## Testing layers

- Unit: arithmetic, identifiers, graph mapping, transactions, codecs.
- Integration: resource reload, registries, networking, persistence.
- GameTest: blocks, inventories, automation, world lifecycle. Required server GameTests
  run as part of `build` and therefore gate every pull request.
- Smoke: headless dedicated server boot and optional client launch.
- Upgrade: committed fixture worlds/data from every supported release line.
