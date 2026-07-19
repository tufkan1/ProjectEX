# Matter progression safety core

Issue #38 uses loader-neutral transaction/policy classes before any Fabric or Minecraft
world mutation. Runtime items and furnaces must call these classes on the server and
commit only a complete returned plan.

## Tier definitions

`MatterTier` contains mining level/speed, attack bonus, charge and request bounds,
per-block EMC cost, cooldown, furnace timing/output layout, bonus-output probability,
and the armor reduction cap. The built-in dark/red defaults are compatibility-oriented
starting values researched against ProjectE commit
`15d4ce65bd06eb4222709b984255fbf5080e78bc`. A later runtime PR will load the same
validated shape from data packs; invalid or unsafe definitions fail before publication.

## Area action contract

`MatterActionPlanner` accepts a server raycast origin and candidate positions. It:

1. removes duplicates and positions outside the charged radius;
2. sorts nearest-first with stable coordinate tie-breaking;
3. caps the request to the tier maximum;
4. applies the claim/protection predicate to every position;
5. stops before the authoritative EMC balance would be exceeded; and
6. returns a cooldown only when at least one position can commit.

The world executor must revalidate the plan immediately before mutation, charge the
exact returned EMC once, and emit one privacy-minimal `MatterActionAuditEvent`. Claim
mods can register `MatterAreaActionProtection.EVENT`; the runtime executor will issue
an immutable `MatterAreaActionContext` for every candidate before it enters the plan.

## Furnace contract

`MatterFurnaceTransaction` builds an all-or-nothing output slot plan. Component variants
never merge with another variant. Bonus output is included before space validation; if
the complete result does not fit, input is unchanged. Fuel is consumed only when burn
time is positive and any crafting remainder has an already-reserved exact sink.

## Armor contract

`MatterArmorPolicy` calculates damage only on the server, honors bypass-armor damage,
caps reduction at 95%, and rate-limits full-set periodic effects to at most once per
20 ticks. The built-in tiers deliberately remain below the global cap, so armor can
never turn a positive incoming hit into invulnerability.
