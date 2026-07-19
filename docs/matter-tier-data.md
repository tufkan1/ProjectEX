# Matter tier data-pack format

ProjectEX loads the winning JSON resources below `data/<namespace>/projectex/matter_tiers/`
on every server data reload. Both built-in tier files use schema version 1. Preparation
is fail-closed and publication is atomic: if any winning definition is malformed or
unsafe, the reload fails and the previous complete snapshot remains active.

```json
{
  "schema_version": 1,
  "id": "dark_matter",
  "mining_level": 3,
  "mining_speed": 14.0,
  "attack_bonus": 3.0,
  "max_charge": 4,
  "max_area_blocks": 125,
  "action_cooldown_ticks": 8,
  "emc_per_area_block": "64",
  "furnace_cook_ticks": 10,
  "furnace_output_slots": 9,
  "bonus_output_numerator": 1,
  "bonus_output_denominator": 2,
  "armor_damage_reduction_cap": 0.8
}
```

Only `dark_matter` and `red_matter` are accepted in schema 1. Unknown and missing
fields are errors. Numeric safety limits come from `MatterTier`; runtime output slots
are additionally capped at 18, bonus numerator cannot exceed its denominator, EMC is
a canonical non-negative integer string of at most 40 digits, and all decimal values
must be finite.

The server reads the active snapshot at action time. Mining speed, charge/radius,
request cap, cooldown, EMC cost, armor policy, furnace time, output layout, and bonus
probability therefore update without replacing blocks or items. Existing tool charge
is clamped after a pack lowers the maximum. Vanilla registration-bound harvest tags,
durability, base attack-speed modifiers, and armor slot defense remain fixed for a
running registry; base attack-damage modifiers are registration-bound as well. These
fields require a server restart/mod build change rather than unsafe
live registry mutation.

Pack authors should copy both bundled files, change only deliberate balance values,
run `/reload`, and review the server log entry confirming two validated definitions.
