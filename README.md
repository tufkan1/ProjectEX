# ProjectEX

ProjectEX is a community-driven alchemy and EMC mod being built for Fabric,
starting with Minecraft Java Edition 26.2. It is a clean, maintainable
implementation informed by the behavior of ProjectE and ProjectExpansion.

> **Status: early alpha.** The EMC platform and command-opened transmutation vertical
> slice are playable, but content progression and world blocks are still incomplete.
> Do not use development builds in important worlds.

The current playable loop starts at the craftable Transmutation Table. Right-click it,
learn a held item, burn valued items into EMC, then create learned items from the
server-authoritative browser. `/projectex transmutation` remains available as a
development fallback.

## Compatibility

| Component | Current target |
| --- | --- |
| Minecraft | 26.2 |
| Fabric Loader | 0.19.3 or newer |
| Fabric API | 0.155.2+26.2 |
| Java | 25 |
| Gradle | Wrapper 9.6.1 |

Each Minecraft feature release receives an explicit compatibility decision and
CI validation. “26.2+” means the project will actively port forward; it does not
mean one jar is assumed to work forever. See [support policy](docs/support-policy.md).

## Build locally

Install JDK 25, then run:

```text
./gradlew build
```

Windows PowerShell:

```text
.\gradlew.bat build
```

Artifacts are written to `build/libs`. Run unit tests with `./gradlew test` and
launch a development client with `./gradlew runClient`.

## Project map

- [Roadmap](ROADMAP.md) — delivery phases and release gates
- [Task backlog](TASKS.md) — issue-ready work packages and acceptance criteria
- [Architecture](docs/architecture.md) — module boundaries and technical rules
- [EMC data format](docs/emc-data-format.md) — versioned pack-author format and conflict rules
- [Public EMC API](docs/emc-api.md) — query lifecycle, consumer examples, and compatibility policy
- [EMC storage API](docs/emc-storage-api.md) — portable storage, exact transfers, automation, and migration
- [EMC machine core](docs/machine-core.md) — fixed-point generation, ownership, budgets, and cycle safety
- [Player data](docs/player-data.md) — persistence schema, lifecycle, and corruption recovery
- [Alchemy transactions](docs/alchemy-transactions.md) — atomic learn, burn, create, and abuse controls
- [Network protocol](docs/network-protocol.md) — payload schema, sessions, replay defense, and limits
- [Transmutation UI](docs/transmutation-ui.md) — keyboard, narration, paging, and latency contract
- [Content and datagen](docs/datagen.md) — registration, generated resources, and verification
- [Philosopher's Stone](docs/philosophers-stone.md) — controls, protection hooks, and transaction rules
- [Contributing](CONTRIBUTING.md) — development and pull request workflow
- [Support](SUPPORT.md) — where questions and bug reports belong
- [Security](SECURITY.md) — private vulnerability reporting policy

## Independence and attribution

ProjectEX is not supported or endorsed by ProjectE or ProjectExpansion. Please
report ProjectEX problems only in this repository. Both reference projects are
MIT-licensed; provenance rules are documented in [NOTICE](NOTICE) and
[docs/provenance.md](docs/provenance.md).

## License

ProjectEX is available under the [MIT License](LICENSE).
