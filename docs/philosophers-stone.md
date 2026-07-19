# Philosopher's Stone

The Philosopher's Stone is ProjectEX's first server-authoritative active item. Its
stack size is one and its state is stored in the versioned `active_item_state` data
component. The component persists `version`, `charge`, and `mode`, and uses a bounded
network codec so the server and observing clients see the same item state.

## Controls

- Use in air to cycle charge from 0 through 2.
- Sneak-use in air to cycle Cube, Panel, and Line modes.
- Use a supported block to move forward through the transformation cycle.
- Sneak-use a supported block to move backward through the cycle.

The default cycle is Stone, Cobblestone, Gravel, Sand, Grass Block, and Dirt. Charge
controls radius; mode controls whether the plan is a volume, clicked-face panel, or
horizontal line.

## Authority and protection

The client never sends a ProjectEX world-mutation payload. Normal Minecraft item-use
handling reaches the server, which computes the target positions and states again.
Before changing anything, the server requires every mapped position to pass all gates:

1. the chunk is loaded and the block has no block entity;
2. the original state is in `projectex:philosophers_stone_allowed`;
3. the original state is not in `projectex:philosophers_stone_denied`;
4. the replacement can survive at the target position;
5. vanilla spawn/adventure interaction checks allow the player action;
6. every registered `WorldTransmutationProtection` callback approves it.

Claim and protection integrations should register a callback on
`io.github.tufkan1.projectex.api.alchemy.WorldTransmutationProtection.EVENT` and return
`false` for a denied context. Callbacks
receive the server level, player, catalyst stack, immutable position, original state,
and proposed state. They run during preflight, before any block is changed.

## Atomicity

Unsupported blocks are ignored. A denied mapped position rejects the complete plan.
After preflight, changes commit in deterministic coordinate order. If Minecraft rejects
any write, ProjectEX restores every already-written original state in reverse order and
reports a rolled-back result. Successful uses receive a short server-managed cooldown.

Data packs may append to the allow and deny tags, but Java transformation pairs remain
an explicit safe list. Adding a new tag entry alone cannot invent an arbitrary target
state.
