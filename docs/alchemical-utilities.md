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

## Remaining #39 families

Pedestal effects, repair talisman, divining rods, amulets, rings, knowledge tome policy, and
destructive catalysts will build on this state/network contract. Their world adapters must
add per-target protection callbacks, allow/deny tags, chunk-loaded bounds, per-tick work caps,
and exact EMC charging before #39 is closed.
