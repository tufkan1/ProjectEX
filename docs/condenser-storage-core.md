# Condenser and portable storage transaction core

Condensers use a loader-neutral evaluator before any Minecraft inventory mutation.
The evaluator receives an exact component-aware target, immutable inputs, existing
buffer EMC, output capacity, and a per-tick input budget. It returns consumed counts,
produced count, and retained EMC as one commit plan.

The mandatory conservation equation is:

```text
initial buffer + sum(consumed item EMC) = final buffer + produced target EMC
```

Targets with zero EMC, negative budgets, component substitutions, and output
overflow are rejected. A full output consumes nothing. Stateless callers default to
not consuming a sub-target first input; persistent condenser block entities explicitly
opt into buffering that EMC. This distinction lets MK1 accumulate low-value items
without allowing a portable caller to strand value unexpectedly.

## Component identity

`CondenserVariant` contains the registry item identifier and canonical persistent
component JSON. Two stacks with the same item but different potion, enchantment, or
other persistent components are distinct variants. Target stacks are never burned as
ordinary input.

## Bag identity and nesting

Every alchemical bag receives a versioned random UUID, color key, and optional owner.
Access permits the owner or an operator override. The nesting policy rejects the same
bag identity and every registered portable-container item before insertion commits,
preventing direct and indirect recursive inventories.

## Fabric runtime contract

- MK1 consumes at most one item per server tick, MK2 at most 64, and MK3 at most
  512. MK3 has 91 input and 180 output slots, exposed through six server-owned pages;
  its target remains a separate, non-automatable template slot.
- Slot 0 is a physical target template and is never exposed to automation.
- Horizontal faces insert only into the 42 input slots. Vertical faces extract only
  from the 42 output slots.
- Stateful targets never fall back to a componentless EMC value. They require an
  explicit exact component match, preventing charged or container items from being
  cloned at their base item value.
- The 104-slot Alchemical Chest and both condenser inventories retain their complete
  container, exact buffer, owner, and access flag across save/reload and break/place.
- Menus page on the server and expose one stable slot map at a time.

## Advanced Alchemical Chest

The advanced chest expands storage to 243 slots over five server-owned pages. It reuses
the same owner/operator access checks, comparator fullness calculation, block-item
container component, and Fabric sided transfer adapter as the base chest. Its smithing
upgrade treats the Alchemical Chest as the base stack, so vanilla's component-preserving
transform carries all 104 legacy slots and ownership into the larger layout; the 139 new
slots start empty. Break/place remains lossless across all 243 slots.

Opening the advanced chest applies a deterministic item-id/component sort over at most
243 stacks. Its insertion policy carries at most 64 registry identifiers and cycles
between allow-all, allow-list, and deny-list modes. The owner configures it in-world:
sneak-use with an empty hand cycles the mode, while sneak-use with an item toggles that
item in the list. Player menu insertion and Fabric hopper insertion share the same
policy check. Existing contents are never deleted when a policy changes, and sorting
does not merge or mutate stacks.

## Bag persistence and copy rule

All 16 dye colors create a versioned UUID, owner, and paged 104-slot inventory on first server use. Contents are
stored in the overworld `projectex_alchemical_bags` saved-data file by UUID, not copied
onto the carrier stack. A legacy stack component is imported once and removed. If a
stack is copied by an operator or external mod, both carriers open the same shared
inventory; no second item store is created. Bags reject every `AlchemicalBagItem`, so
same-id, cross-color, and indirect portable nesting all fail before mutation.
