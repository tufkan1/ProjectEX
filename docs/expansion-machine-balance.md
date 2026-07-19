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

## Runtime content contract

- Basic, dark, and red collectors/relays reuse the baseline MK1-MK3 blocks.
- Magenta through final collectors and relays are forward-only upgrades that keep
  the existing versioned machine state and carried-item component format.
- All 16 power flowers generate through the same exact fixed-point accumulator.
- A Compact Sun immediately below a power flower applies the validated server
  multiplier. It does not chunk-load and does not alter persisted EMC.
- Nine collectors craft one compressed collector; two compressed collectors and
  six matching relays form a power flower. The central recipe slot uses the
  Transmutation Table until the EMC Link family lands in #42, after which datagen
  can switch the ingredient without changing machine state or world data.

The Compact Sun multiplier is configured with the JVM property
`projectex.machine.compactSunMultiplier` (`0` disables the bonus; valid range
0-1,000,000; default 10). Invalid values fail startup/reload instead of silently
changing generation.

No block-state migration is required for existing MK1-MK3 machines. Expansion
machines use the same schema-v1 fields and strict tier-bound decoding, so a block
item cannot be placed into a different tier to increase its stored capacity.

Collectors upgrade the complete forward fuel chain:

`Alchemical Coal -> Mobius Fuel -> Aeternalis Fuel -> Magenta ... -> White Fuel`

Every boundary reads the current data-pack EMC values and spends exactly
`output - input`; missing, reversed, or unaffordable mappings commit nothing.
Changing a rate multiplier changes generation/transfer throughput only and never
changes recipe value or an already persisted machine balance.
