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
| Dark/red matter blocks, tools, and armor | Implemented | ProjectE | Data-pack runtime balance, server-raycast area mining, exact EMC payment/refund, public claim hook, bounded cooldowns, vanilla-attribute armor |
| Dark/red matter furnaces | Implemented | ProjectE | Atomic multi-slot output, component identity, exact fuel remainder, sided Fabric automation, narrated progress UI |
| Expansion collectors, relays, power flowers, and Compact Sun | Implemented | ProjectExpansion | Exact fixed-point/BigInteger accounting, schema-v1 carry state, bounded multiplier, no chunk tickets, original vanilla-reference models |
| EMC Links and Transmutation Interface | Implemented | ProjectExpansion | 16 tiered links, sided transactional Fabric storage, bounded knowledge views, owner/member access, filters, persistence, and management UI |

Collector/relay baseline rates and capacities follow ProjectE commit
`15d4ce65bd06eb4222709b984255fbf5080e78bc`. All Fabric integration, persistence,
network routing, menus, tests, models, and textures in ProjectEX are original.
Expansion machine identities and balance formulas follow ProjectExpansion commit
`168bcf2491b9fde679295fd412ad9c93fd3d93f1`; no upstream art is distributed.
