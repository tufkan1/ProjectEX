# Behavior parity matrix

This matrix records intentional ProjectEX behavior against the reference projects.
“Compatible” means the player-facing concept/progression is recognizable; it does
not imply copied implementation or byte-for-byte behavior.

| Feature | ProjectEX status | Reference | Intentional differences |
| --- | --- | --- | --- |
| EMC values and recipe mapping | Implemented | ProjectE | Arbitrary precision, deterministic data-pack conflicts, cycle/free-EMC rejection |
| Player EMC and knowledge | Implemented | ProjectE | Versioned UUID save, atomic server transactions, bounded network protocol |
| Transmutation Table | Implemented | ProjectE | Search/favorites accessibility and server session/replay protection |
| Philosopher's Stone | Baseline implemented | ProjectE | Tag policy and public claim-protection hook; additional modes remain planned |
| Klein Stars Ein–Omega | Implemented | ProjectE | Public Fabric item lookup and exact retained-EMC upgrade recipe |
| Energy Collectors MK1–MK3 | Implemented | ProjectE | Integer/fixed-point budget, ownership, redstone modes, two-slot accessible UI |
| Anti-Matter Relays MK1–MK3 | Implemented | ProjectE | Directed-cycle rejection, world tick budget, Fabric sided storage |
| Condensers MK1/MK2 | Implemented | ProjectE | Exact persistent buffer, component-safe targets, separated sided automation, server paging |
| Alchemical Chest and Bags | Implemented | ProjectE | 104-slot chest and bags; copied bag identities mirror one UUID/world-backed inventory |
| Advanced tiers and links | Planned | ProjectExpansion | No content or assets imported; scope will be reviewed per feature |

Collector/relay baseline rates and capacities follow ProjectE commit
`15d4ce65bd06eb4222709b984255fbf5080e78bc`. All Fabric integration, persistence,
network routing, menus, tests, models, and textures in ProjectEX are original.
