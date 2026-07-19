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
overflow are rejected. A full output consumes nothing. If an empty condenser cannot
produce even one target from the permitted input budget, it also consumes nothing;
this prevents automation from unexpectedly stranding partial value on first insert.

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
