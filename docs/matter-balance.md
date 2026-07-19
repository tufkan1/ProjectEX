# Matter progression balance

These values are public compatibility defaults, not hidden implementation constants.
Changes require tests, changelog notes, and an explicit balance review.
Servers may override runtime-safe fields through the strict schema documented in
`matter-tier-data.md`.

| Property | Dark matter | Red matter |
| --- | ---: | ---: |
| Tool durability | 4,096 | 8,192 |
| Mining speed | 14 | 16 |
| Maximum charge | 4 | 5 |
| Maximum blocks per action | 125 | 343 |
| EMC per committed area block | 64 | 32 |
| Successful action cooldown | 8 ticks | 6 ticks |
| Armor durability multiplier | 48 | 64 |
| Armor defense (head/chest/legs/feet) | 3/8/6/3 | 4/9/7/4 |
| Toughness / knockback resistance | 3 / 0.1 | 4 / 0.2 |
| Complete-set periodic effect | air + fire | air + fire + 0.5 health |
| Furnace cook time | 10 ticks | 5 ticks |
| Furnace output slots | 9 | 18 |
| Furnace bonus output | 50% | 100% |

Area candidates are nearest-first and deterministically capped. EMC and durability are
charged only for blocks that the server actually breaks. A protected block never
consumes either resource. Red matter being cheaper per area block is intentional late
progression acceleration; it does not increase the player's EMC balance or bypass
claim integrations.

Matter furnaces consume vanilla fuels at their registered burn duration. Faster tier
cook times therefore increase work per fuel item without inventing fuel energy. A
crafting remainder is committed only when its exact destination is available.
