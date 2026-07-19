# Roadmap

Roadmap order is deliberate: data correctness and save safety precede content
volume. Dates are assigned only after a maintainer accepts an issue.

## M0 — Repository foundation (current)

- [x] Fabric 26.2 / Java 25 / Loom 1.17 build
- [x] Unit-tested arbitrary-precision EMC primitives
- [x] CI, release, contribution, issue, security, and governance scaffolding
- [ ] First public build succeeds on GitHub Actions
- [ ] Repository labels, branch protection, Discussions, and private security reporting enabled

Exit gate: clean CI on `main`, reproducible jar, all community links operational.

## M1 — EMC data platform

- Data-pack JSON schema for explicit values, removals, aliases, and priorities
- Server reload listener with validation and deterministic conflict resolution
- Recipe graph mapper with cycle/overflow protection
- `/projectex emc`, reload, and diagnostics commands
- Generated default EMC data and validation report
- Public API and GameTest coverage

Exit gate: EMC results are deterministic across reloads, servers, and mod order.

## M2 — Player knowledge and transmutation

- Persistent per-player EMC balance and learned-item knowledge
- Server-authoritative networking with validation and rate limits
- Transmutation table/tablet menu, screen, search, learn, burn, and create flows
- Death/clone/dimension lifecycle behavior and migration tests
- Operator recovery and audit commands

Exit gate: multiplayer tests pass without duplication or item-loss defects.

## M3 — Core alchemy content

- Fuels and matter tiers
- Alchemical coal/fuel progression and crafting recipes
- Klein-star-style portable EMC storage
- Condenser, collector, relay, and chest vertical slices
- Datagen for recipes, loot, models, tags, advancements, and translations

Exit gate: dedicated server smoke tests and world upgrade test fixtures pass.

## M4 — Expansion content

- Advanced collectors, relays, condensers, power flowers, and EMC links
- Matter upgrades, advanced storage, books/tablets, and quality-of-life commands
- Configurable balance profiles; no hard-coded pack policy
- Performance budget and profiling for large automation networks

Exit gate: documented parity matrix meets the agreed 1.0 scope.

## M5 — Ecosystem and 1.0

- EMI integration first; optional REI/Jade/WTHIT integrations as maintained
- Modrinth and CurseForge publishing after ownership/secrets are configured
- Migration guide, modpack guide, API docs, and complete attribution audit
- Release candidate soak period and world backup/migration rehearsal

Exit gate: no open release-blocker issue; security, license, performance, and save
compatibility reviews complete.

## Forward ports

For every Minecraft release after 26.2: create a compatibility issue, update the
toolchain on a dedicated branch, run the full test matrix, document breaking API
changes, and release a Minecraft-specific artifact. See `docs/support-policy.md`.
