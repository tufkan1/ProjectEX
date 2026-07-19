# Implementation backlog

This is the seed backlog. Each `PX-*` section is designed to become one GitHub
issue or a small epic. Maintainers copy it into an issue, add an area/priority
label, and link dependencies. A checked box here is changed only by the PR that
completes the task.

## M0 — Foundation

### PX-001: Verify first public CI build

Depends on: none

- Run wrapper validation, unit tests, build, and artifact upload on Linux.
- Confirm the jar contains `fabric.mod.json`, `LICENSE`, and `NOTICE`.
- Record the required `build` status check in `docs/maintainers/repository-settings.md`.

Acceptance: a pull request cannot merge when `./gradlew check build` fails.

### PX-002: Configure repository settings

Depends on: PX-001

- Create labels from `.github/labels.yml`.
- Enable Issues, Discussions, private vulnerability reporting, and auto-delete branches.
- Protect `main` using the documented ruleset.
- Add Modrinth/CurseForge secrets only when publisher accounts exist.

Acceptance: settings checklist is signed off by a maintainer.

## M1 — EMC platform

### PX-101: Specify EMC data-pack schema

- Define versioned JSON examples and JSON Schema.
- Support explicit value, removal, source priority, and optional item components.
- Reject negatives, unknown items, invalid identifiers, and unsafe numeric sizes.

Acceptance: schema fixtures include valid, invalid, override, and removal cases.

### PX-102: Implement atomic EMC reload

Depends on: PX-101

- Parse on the server reload executor and publish one immutable snapshot.
- Preserve the previous snapshot if validation fails.
- Emit concise diagnostics and a machine-readable report.

Acceptance: concurrent reads never observe a partially loaded registry.

### PX-103: Recipe graph mapper

Depends on: PX-101

- Model crafting/smelting-like recipe inputs with configurable exclusions.
- Find deterministic minimum values without feedback loops or free-EMC exploits.
- Bound iterations and arbitrary-precision growth.

Acceptance: cycle, ambiguity, remainder, container-item, and tag tests pass.

### PX-104: EMC commands and permission policy

Depends on: PX-102

- Query held/item identifier values.
- Reload and dump diagnostics for operators.
- Keep mutations behind explicit permission levels and audit logs.

Acceptance: commands work on a dedicated server and reject unauthorized users.

## M2 — Transmutation

### PX-201: Persistent player alchemy state

- Define versioned save codec for balance and knowledge.
- Specify clone/death/logout/dimension transfer behavior.
- Add corruption recovery and migration fixtures.

Acceptance: round-trip and upgrade tests cover normal and maximum-size balances.

### PX-202: Server-authoritative transaction service

Depends on: PX-201, PX-102

- Implement learn, burn, and create as atomic transactions.
- Validate inventory, knowledge, balance, menu distance, and request rate.
- Never trust client-provided EMC amounts.

Acceptance: adversarial packet tests cannot duplicate items or EMC.

### PX-203: Transmutation UI and search

Depends on: PX-202

- Implement menu/screen, paging, search, favorites, and accessibility narration.
- Synchronize only required state and handle reconnect/reload gracefully.

Acceptance: keyboard-only and multiplayer latency test cases are documented.

## M3/M4 — Content epics

### PX-301: Fuel and matter progression
### PX-302: Portable EMC storage
### PX-303: Condenser vertical slice
### PX-304: Collector and relay networks
### PX-305: Alchemical storage and tools
### PX-401: Advanced machine tiers
### PX-402: Power flowers and EMC links
### PX-403: Books, tablets, upgrades, and utility content

Each content issue must include behavior references, original design decisions,
asset provenance, recipes/tags/loot/models/translations, GameTests, server test,
balance configuration, and parity-matrix updates.

## Cross-cutting epics

### PX-501: Datagen and generated-resource verification
### PX-502: Networking protocol version and fuzz tests
### PX-503: Performance benchmarks and budgets
### PX-504: Save/data migration framework
### PX-505: EMI and information-overlay integrations
### PX-506: Localization workflow (English source, Turkish maintained)
### PX-507: Modrinth/CurseForge release automation
### PX-508: Forward-port playbook for 26.3+

## Definition of done

Every implementation issue requires tests proportional to risk, dedicated-server
compatibility, documentation, localization keys, provenance notes, changelog entry,
no new compiler warnings, and a reviewed migration impact statement.
