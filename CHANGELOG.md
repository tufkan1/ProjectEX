# Changelog

All notable changes are documented here. The format follows Keep a Changelog,
and versions follow Semantic Versioning for the mod release independently of
the Minecraft version.

## [Unreleased]

## [26.3.4] - 2026-07-24

### Added

- Rebuilt Minecraft 26.3 Snapshot 1-4 artifacts from the ProjectEX 1.1.6 source,
  including the optional JEI recipe-transfer bridge and its Fabric plugin entrypoint.
- A paired-release checklist requirement so compatible stable changes are not published
  without refreshed 26.3 snapshot artifacts.

### Notes

- JEI has not published a Minecraft 26.3 Fabric runtime at release time. The bridge is
  included for forward compatibility, but JEI-present runtime verification remains limited
  to Minecraft 26.2 until an official 26.3 artifact exists.

## [1.1.6] - 2026-07-23

### Added

- Native JEI 30.13 Fabric integration for Minecraft 26.2, including a registered
  `jei_mod_plugin` entrypoint and Arcane Crafting recipe-transfer handler.
- A JEI-present client GameTest that resolves the live transfer handler for the portable
  3x3 grid, plus absent/present integration gates in CI and the release workflow.

### Fixed

- JEI crafting recipes now show the transfer `+` button while Arcane Crafting is open and
  can move ingredients between player inventory slots 10-45 and recipe slots 1-9.

## [1.1.5] - 2026-07-20

### Added

- Remappable `V` charge, `G` mode, and `K` inventory Arcane Tablet controls in Minecraft's
  native ProjectEX key-binding category, with server validation for remote tablet opening.
- An optional Mod Menu settings entrypoint for EMC tooltips, compact number formatting,
  remembered favorites, and transmutation-search autofocus.
- Client render traversal for every currently registered machine/storage menu and the new
  settings screen, plus present/absent optional-integration boot verification.

## [1.1.4] - 2026-07-20

### Added

- Runtime coverage for every registered block placement, native menu slot counts, and
  resolvable blockstate/model/texture dependency chains.
- Dedicated Minecraft 26.3 Snapshot 1-4 build profiles, compatibility shims, runtime
  test coverage, and separately named release artifacts under the `26.3` prerelease.
- A substantially expanded vanilla EMC seed catalog based on Tekkit Classic and modern
  ProjectE data, including explicit values for uncraftable post-1.21 content.

### Changed

- Alchemical storage, condensers, collectors, relays, and transmutation now use the
  original ProjectE/ProjectExpansion panel sizes and slot coordinates without custom
  access strips or storage pagination.
- Condenser MK3 opens its 13x7 input panel from five faces and its 20x9 output panel
  from the bottom face, matching ProjectExpansion.
- Collector and baseline relay inventories now expose their complete tier-specific
  source layouts; expansion relays, power flowers, EMC Links, and the Transmutation
  Interface no longer open management containers that do not exist upstream.
- Crafted vanilla items continue to derive EMC from loaded recipe ingredients; explicit
  catalog entries are reserved for primitives, uncraftable content, and authoritative
  compatibility overrides.
- Snapshot 4 uses its new cooking-fuel components, input constants, recipe holder sets,
  and loot modifiers while retaining exact machine inventories and ownership on break/place.

### Fixed

- Placed condenser and alchemical chest models now inherit block models directly instead
  of traversing item-model parents, preventing broken in-world geometry or missing faces.
- Condenser and collector target slots behave as non-consuming templates, and machine
  output slots reject insertion as in the source mods.

## [1.1.3] - 2026-07-20

### Added

- ProjectE Alchemical Coal, Mobius Fuel, and Aeternalis Fuel blocks with original
  models/textures, reversible compression recipes, exact EMC, loot, tags, and fuel times.
- Regression coverage for held-item storage opening, the complete red matter furnace
  slot layout, visible fuel progress, blocked outputs, and coal collector upgrades.

### Fixed

- Red Matter Furnace now exposes all 13 inputs and exactly 13 outputs; legacy 18-output
  inventories migrate without deleting their five formerly invisible stacks.
- Matter furnaces stop before consuming fuel or input when every visible output is full,
  and display the remaining burn flame from synchronized server state.
- Alchemical Chests and all three Energy Condensers open while the player holds an item.
- Energy Collectors accept coal and upgrade it through Alchemical Coal, Mobius Fuel,
  Aeternalis Fuel, and the ProjectExpansion fuel sequence.
- Condenser MK3 and Advanced Alchemical Chest now have their missing loot and mining data.

## [1.1.2] - 2026-07-20

### Added

- A runtime coverage gate now fails whenever any registered `projectex:*` item lacks
  a componentless EMC value after data and recipe mapping.
- ProjectE-compatible vanilla seed values needed by the complete ProjectEX recipe graph.

### Changed

- Item tooltips compact values of one million or more with readable suffixes, so
  `74488428` is displayed as `74.4M`; smaller values keep grouped exact digits.
- Dynamic Klein Star upgrades now participate in EMC recipe mapping across every
  ProjectE and ProjectExpansion star tier.

## [1.1.1] - 2026-07-19

### Fixed

- Energy Condenser and alchemical chest block/item models now inherit the original
  full chest geometry and rotate with their placement direction.
- Condenser, alchemical storage, collector, relay, matter-furnace, transmutation,
  alchemical-book, and automation screens no longer stretch, tint, or cover source artwork.
- Menu slot coordinates now use the source tier's native panel dimensions from the moment
  the server opens the screen; ProjectEX access and redstone controls live outside source panels.
- Matter furnaces expose only their tier's original output layout instead of drawing eighteen
  overlapping output positions.

## [1.1.0] - 2026-07-19

### Added

- Server-authoritative EMC values beneath every unmodified valued item tooltip, refreshed
  on connection and after datapack/recipe EMC reloads.
- Original MIT-licensed ProjectE and ProjectExpansion item, block, and panel visuals with
  pinned upstream provenance and licenses embedded in every release jar.

### Changed

- Release artifact names now include the Minecraft target before the mod version, such as
  `projectex-fabric-26.2-1.1.0.jar`.
- Accessible ProjectEX controls now render over the corresponding original source panels.
- Documented the project's entirely AI-assisted development model and user risk notice.

## [1.0.0] - 2026-07-19

### Changed

- Froze the Minecraft 26.2 support matrix, public API, configuration schema 1, and
  persisted-data format 1 after the complete release audit and public alpha rehearsal.
- Promoted the same audited feature set to the first world-supported stable release;
  operators upgrading from `0.1.0-alpha.1` receive the documented format-0 migration.

## [0.1.0-alpha.1] - 2026-07-19

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
