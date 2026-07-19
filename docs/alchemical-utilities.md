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

## Knowledge Tome policy

Using a Knowledge Tome atomically learns every positive, item-only entry from one immutable
server EMC snapshot while preserving the player's balance and existing component-specific
knowledge. Survival use consumes the tome; creative use does not. A no-op or failed
compare-and-set consumes nothing.

Servers select `projectex.knowledgeTome.policy=consume`, `operator_only`, or `disabled` as a
JVM property. The default is `consume` for recognizable survival progression. Unknown values
fail startup instead of silently enabling this high-impact power. The learned set is checked
against the persistent 100,000-entry safety cap before mutation.

## Destructive catalysts

Nova Catalyst is a consumed, entity-safe 3x3x3 demolition charge. Destruction Catalyst is a
reusable directional tool: charge selects depth 1, 4, or 8, while line/panel/cube mode selects
a 1-, 3-, or 9-block cross-section. A request can therefore consider at most 72 positions.
Destruction Catalyst pays exactly 8 EMC per block that is actually removed and refunds any
planned work that fails during commit.

Both tools operate only on already-loaded chunks, skip block entities and hardness 50+
blocks, require normal player edit permission, and apply
`projectex:destructive_catalyst_allowed`, `projectex:destructive_catalyst_denied`, and the
public utility protection callback to every target. They never create an explosion entity or
damage entities. `projectex.destructiveCatalysts.enabled=false` disables both before planning,
payment, mutation, cooldown, or item consumption; malformed values fail startup.

## Vitality stones

Body Stone restores two hunger points for 64 EMC, Soul Stone restores two health points for
64 EMC, and Life Stone combines only the effects currently needed. When both are needed, its
128 EMC debit is atomic: insufficient funds apply neither effect. No-op and failed actions
consume nothing, and successful use has a 20-tick cooldown.

All three stones work in the Dark Matter Pedestal every 20 ticks. The pedestal considers at
most 16 living, non-spectator players in already-loaded chunks, sorted by UUID. Body and Soul
restore one hunger or health point respectively; Life applies both where needed. These effects
use no EMC because pedestal ownership, range, redstone, and work caps are the balancing
boundary.

## Remaining #39 families

Future utility additions must preserve the same protection callbacks, allow/deny tags,
chunk-loaded bounds, per-tick work caps, and exact EMC charging established by #39.
