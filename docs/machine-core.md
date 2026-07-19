# EMC machine core

The machine core is the loader-neutral transaction boundary used by collectors,
relays, and future EMC blocks. Minecraft block entities adapt their persisted and
world state into these types; they do not implement separate arithmetic rules.

## Invariants

- Machine buffers use arbitrary-precision `EmcValue` and reject stored values above
  capacity. No load or transaction path clamps, wraps, or silently repairs EMC.
- Generation rates are exact rational numbers. Fractional and tick-budget-deferred
  numerators are persisted so unloads cannot lose or create value.
- A per-tick budget limits both transfer count and total moved EMC.
- The tick-local network ledger rejects self-routes and any edge that would complete
  a directed cycle. Its sorted graph and iterative traversal are deterministic and
  safe for maximum-size networks. Every accepted move subtracts and inserts the
  same exact value.
- Adjacent routing checks only already-loaded chunks. A machine tick never requests
  or retains a chunk ticket, and unload boundaries defer transfer without losing EMC.
- Ownership is explicit. Unclaimed machines may be claimed; private machines permit
  their owner and operator overrides; public access is owner-controlled.
- Redstone behavior is one of ignored, signal-required, or no-signal-required and is
  evaluated only on the logical server.

## Baseline balance

| Machine | EMC/tick or transfer/tick | Capacity |
| --- | ---: | ---: |
| Collector MK1 | 4 | 10,000 |
| Collector MK2 | 12 | 30,000 |
| Collector MK3 | 40 | 60,000 |
| Relay MK1 | 64 | 100,000 |
| Relay MK2 | 192 | 1,000,000 |
| Relay MK3 | 640 | 10,000,000 |

These are compatibility-oriented starting values. Pack/server configuration may
apply stricter tick budgets, but cannot bypass capacity or conservation checks.

Global network work can be bounded with the server JVM properties
`projectex.machine.maxTransfersPerWorldTick` (1–1,000,000) and
`projectex.machine.maxEmcPerWorldTick` (a positive decimal EMC value). Invalid
values fail startup instead of silently disabling safety limits.

The verified maximum topology profile is 10,000 loaded nodes with 9,999 accepted
edges and one rejected cycle-closing edge in a single ledger. The stress test
uses a 10,000-transfer/10,000-EMC budget and proves exact conservation without
recursive graph traversal. Larger physical networks remain safe because the
world budget stops additional routes, but 10,000 loaded nodes is the supported
performance envelope until a larger profile is added to CI.

Adjacent transfers query only the server chunk source. An unloaded neighbor is
not routed to and no chunk ticket is created; the source retains its EMC. Stored
EMC and the fractional generation numerator are both schema-v1 fields, so a
save/reload at a chunk boundary resumes with the exact pre-unload value.

## Exact rate configuration

The following JVM properties accept either a decimal (`1.5`) or a rational
(`3/2`) between `0.001` and `1000000`:

- `projectex.machine.collectorRateMultiplier`
- `projectex.machine.relayTransferMultiplier`
- `projectex.machine.powerFlowerRateMultiplier`

Rates never pass through floating point. Configuration reload builds and
validates one immutable snapshot before publishing it; one malformed property
cannot partially update a running server. Fractional production remains in the
persisted numerator until it becomes whole EMC.

## Persistence

Machine state schema version 1 stores tier identity, exact EMC, deferred generation,
optional owner UUID, public/private access, and redstone mode. Decoding is strict,
tier-bound, and limited to 4096 decimal digits. A malformed state is rejected at the
migration boundary for explicit recovery by the Minecraft adapter.

## Fuel upgrades

A collector fuel upgrade declares input/output identities and their EMC values. Its
cost is exactly `output - input`; rules that reduce value are invalid. The upgrade
commits only when the item matches and the machine buffer can pay the full cost.

## Minecraft block contract

All six baseline tiers are registered blocks backed by one versioned block-entity
type. Collectors generate their exact tier rate, upgrade Alchemical Coal to Mobius
Fuel and Mobius Fuel to Aeternalis Fuel, charge a Klein Star in the output slot, and
then offer remaining EMC to adjacent machines. Relays burn valued input items or
extract from a portable EMC store, charge the output store, and route the remainder.

Horizontal automation exposes only the input slot. Top and bottom automation expose
only the output slot through Fabric Transfer API. Comparator output is proportional
to the internal EMC buffer. The menu synchronizes integer EMC/capacity/tier values
from the server and contains no client-side transaction path.

Breaking a machine copies versioned machine state and container contents onto the
block item. Placement restores both through vanilla data components, preserving EMC,
owner, redstone mode, deferred generation, and inventory without a second loose-item
drop.
