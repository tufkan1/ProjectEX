# Expansion machine balance catalog

ProjectEX keeps the 16 ProjectExpansion matter levels (`basic` through `final`)
in `ExpansionMachineTier`. This loader-neutral catalog is the single balance
source for collector, relay, power-flower, and Compact Sun block entities.

All values use arbitrary-precision EMC and exact fixed-point tick rates. Nothing
is converted through `double`, clamped to a Java primitive, or discarded when a
per-tick value has a remainder.

## Baseline formulas

For zero-based tier index `n`:

- collector output per second: `4 * 6^n`
- relay bonus per second: `1 * 6^n`
- relay transfer per tick: `64 * 6^n`
- power-flower output per second: `18 * collector + 30 * relay bonus`

The final relay transfer retains the upstream saturation value of
`Long.MAX_VALUE`; storage and accounting remain arbitrary precision. Runtime
multipliers will be applied as validated rational values in the Minecraft
adapter, never by modifying this compatibility baseline.

Compressed collectors are recipe components, not independent generators, so
they deliberately do not expose a second generation rate.
