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
  a directed cycle. Every accepted move subtracts and inserts the same exact value.
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

## Persistence

Machine state schema version 1 stores tier identity, exact EMC, deferred generation,
optional owner UUID, public/private access, and redstone mode. Decoding is strict,
tier-bound, and limited to 4096 decimal digits. A malformed state is rejected at the
migration boundary for explicit recovery by the Minecraft adapter.

## Fuel upgrades

A collector fuel upgrade declares input/output identities and their EMC values. Its
cost is exactly `output - input`; rules that reduce value are invalid. The upgrade
commits only when the item matches and the machine buffer can pay the full cost.
