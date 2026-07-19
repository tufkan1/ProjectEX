# Changelog

All notable changes are documented here. The format follows Keep a Changelog,
and versions follow Semantic Versioning for the mod release independently of
the Minecraft version.

## [Unreleased]

### Added

- Optional Jade 26.2 machine and condenser tooltips with server-side authorization, a privacy allowlist, and present/absent production-client CI profiles.
- Complete player, server-operator, pack-author, integration, accessibility, and translation documentation with generated public-API Javadocs and a compiled example mod.
- Changed player saved data to a backward-compatible chunked NBT representation so large knowledge sets cannot exceed the NBT UTF field limit.
- Fabric 26.2 project foundation using Java 25, Loom 1.17, and Gradle 9.5.1.
- Loader-neutral, arbitrary-precision EMC domain model and unit tests.
- Public repository governance, contribution, security, issue, CI, and release scaffolding.
- Versioned EMC data-pack schema, strict parser, safety limits, and fixture tests.
- Deterministic priority/conflict/alias resolution and atomic server data reload.
- Server-side EMC query, status, reload, and machine-readable diagnostics commands.
- Deterministic recipe-graph EMC mapping core with cycle and free-EMC protection.
- Minecraft 26.2 recipe adapter with safe exclusions and post-reload derivation.
- Versioned query-only EMC API with immutable snapshots and reload subscriptions.
- Versioned UUID-keyed player EMC/knowledge persistence with migration and recovery backup.
- Atomic server-authoritative learn, burn, and create evaluator with access and rate guards.
- Versioned Fabric transmutation payloads with server sessions, replay defense, and bounded codecs.
- Client session cache with authoritative response ordering and disconnect cleanup.
- Bounded server-side knowledge search/paging and client favorites/browser state.
- Command-opened transmutation menu/screen backed by atomic player inventory commits.
- Versioned, bounded client-side persistence for transmutation favorites.
- Craftable Transmutation Table with distance-bound server sessions and starter EMC data.
- Automated server GameTests for physical menu access and runtime EMC reload.
- Deterministic arrow-key result navigation and localized narrated server failures.
- Headless client GameTest for the complete learn, burn, and create network journey.
- Typed content registration and reproducible recipe/tag/loot/model/language/advancement datagen.
- Core covalence dust, fuel, matter progression and a protected server-authoritative Philosopher's Stone.
- Six-tier Klein Star progression with exact retained-EMC upgrades and synchronized tooltips.
- Versioned Fabric item EMC storage API with simulation, automation policy, migration, and overflow safety.
- Exploit-resistant machine core with exact fixed-point generation, ownership/redstone policy, bounded transfers, and cycle rejection.
- Three Energy Collector and Anti-Matter Relay tiers with sided automation, retained break/place state, comparator output, accessible menus, and cycle-safe adjacency transfer.
- Energy Condenser MK1/MK2 and the 104-slot Alchemical Chest with exact EMC conservation, component-safe targets, separated sided automation, retained state, and server-owned paging.
- Sixteen color-keyed Alchemical Bags with owner-bound UUIDs, world-backed shared inventories, legacy component migration, and copy/nesting duplication defenses.
- Data-ready dark/red matter safety core with bounded protection-aware area plans, all-or-nothing furnace output/fuel remainder accounting, armor caps, and audit contracts.
- Craftable dark/red matter blocks, complete tool and armor families, chargeable pickaxe/hammer area mining with exact server EMC accounting, public claim protection, cooldowns, full-set maintenance effects, recipes, tags, models, and English/Turkish localization.
- Dark/red matter furnaces with vanilla recipe compatibility, tier speed and bonus outputs, atomic component-safe output planning, exact fuel remainders, sided automation, retained inventory, and an accessible synchronized progress screen.
- Strict schema-v1 matter-tier data packs with atomic fail-closed reload, bounded balance validation, bundled defaults, live runtime resolution, and safe charge clamping after configuration changes.
- Versioned common/server/client configuration, public-alpha migration fixtures, manifest-backed atomic backups, offline recovery packages, and operator config/migration/datapack/machine audits.
- Reproducible 1.0 performance, allocation, packet, persistence, concurrency, privilege, secret, and artifact release-audit gates.

## [0.1.0-alpha.1] - Unreleased

- Planned first developer preview; not yet feature complete or world-safe.
