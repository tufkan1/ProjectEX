# Alchemical utilities

Issue #39 extends ProjectEX's server-authoritative item model to portable and active utilities.
This document records the shared contract used by every utility family.

## Charge and mode controls

Chargeable utilities persist schema-v1 `ActiveItemState` in the item component. The state is
bounded to charge levels 0–2 and the deterministic cube, panel, and line modes.

- Use in air: cycle charge.
- Sneak + use in air: cycle mode.
- `V` (rebindable): cycle charge for the main-hand utility, otherwise the off-hand utility.
- `B` (rebindable): cycle mode using the same hand priority.

The keybindings send only an action and hand index. The server rejects malformed values,
resolves the currently held stack, requires `ChargeableUtilityItem`, and accepts at most one
state request per player per server tick. The client never supplies an item identity, state,
charge value, or mode value. Every successful change produces accessible overlay feedback.

## Transmutation Tablet

The Transmutation Tablet is a survival-craftable portable access point for the existing M2
transmutation vertical slice. It opens the same menu, server session, revision-pinned EMC
snapshot, paging, favorites, replay protection, and learn/burn/create transactions as the
physical table. There is no second balance or knowledge implementation.

The server authorizes the portable session only while a tablet remains in the hand used to
open it. Dropping, moving, or replacing that held item makes the menu invalid and causes the
session registry to reject later payloads. Disconnect handling is inherited from M2.

## Repair Talisman and Divining Rods

The Repair Talisman repairs one durability point on every damaged inventory item once per
second. Multiple talismans do not stack, and an actively swung main-hand tool is skipped.

The three Divining Rod tiers scan a 3x3 column inward from the clicked face. Their bounded
depths are 3, 16, and 64 blocks, selected through the shared charge state. Scans never load
chunks, do not mutate blocks, honor `projectex:divining_rod_allowed` and
`projectex:divining_rod_denied`, and issue a per-position `UtilityWorldActionProtection`
callback for claim integrations. Results use the live immutable EMC snapshot and report the
average plus the tier's highest distinct values through accessible chat messages.

## Dark Matter Pedestal

The Dark Matter Pedestal is claimed by its placer. Only the owner or a server operator may
insert, extract, activate, or configure its effect. Use with a compatible item to insert one,
use with an empty hand to toggle it, attack to extract it, and sneak-use with an empty hand to
cycle ignored/require-signal/require-no-signal redstone modes. Comparator output distinguishes
empty, unsupported, ready, and active states.

Pedestal effects run only on the logical server. The Repair Talisman effect uses the reference
four-block bounds and a 20-tick interval, considers at most 16 living players per activation,
sorts targets deterministically, and skips players whose chunks are not already loaded. It
never creates chunk tickets. Breaking the pedestal ejects its item before the block entity is
removed.

## Elemental amulets

The Evertide Amulet places an infinite water source while the Volcanite Amulet places lava
for exactly 32 player EMC. Both actions are server-authoritative, affect one already-loaded
position, require normal edit permission, invoke the public utility protection callback, and
require the destination in `projectex:elemental_amulet_allowed` but not
`projectex:elemental_amulet_denied`. A denied or failed action consumes no EMC and leaves no
fluid behind. Evertide does not place water in the Nether.

On an active pedestal, Evertide starts a bounded rain period and Volcanite clears rain and
thunder. Weather effects run every 1,200 ticks and do not scan chunks or entities.

## Remaining #39 families

Additional pedestal effects, rings, knowledge tome policy, and
destructive catalysts will build on this state/network contract. Their world adapters must
add per-target protection callbacks, allow/deny tags, chunk-loaded bounds, per-tick work caps,
and exact EMC charging before #39 is closed.
