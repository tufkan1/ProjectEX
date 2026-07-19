# Source and asset provenance

This file is the auditable ledger for adapted code and assets. Add one row in the
same pull request that introduces any derived material.

| ProjectEX path | Origin project/path | Origin commit | License | Adaptation notes |
| --- | --- | --- | --- | --- |
| `src/main/generated/**` | ProjectEX datagen providers | N/A | MIT | Original ProjectEX data/models; no upstream code or assets copied |
| Core material names and recipe progression | ProjectE content/recipes | `15d4ce65bd06eb4222709b984255fbf5080e78bc` | MIT | Behavior and compatibility research; original Fabric code and vanilla texture references |
| `PhilosophersStoneItem` behavior | ProjectE `PhilosophersStone.java` | `15d4ce65bd06eb4222709b984255fbf5080e78bc` | MIT | Mode/charge concepts researched; transaction, protection, component, and interaction code are original |
| Transmutation Tablet, Repair Talisman, and Divining Rod behavior/recipes | ProjectE `RepairTalisman.java`, `DiviningRod.java`, and generated recipes | `15d4ce65bd06eb4222709b984255fbf5080e78bc` | MIT | Behavior and recipe-shape research only; Fabric session reuse, bounded scan geometry, tag/protection API, non-stacking repair policy, tests, translations, and vanilla-reference models are original ProjectEX work |
| Dark Matter Pedestal behavior and recipe | ProjectE `DMPedestalBlockEntity.java`, `Pedestal.java`, and generated recipe | `15d4ce65bd06eb4222709b984255fbf5080e78bc` | MIT | Range and interaction research only; ownership, explicit redstone policy, deterministic work budget, persistence, tests, and vanilla-reference model are original ProjectEX work |
| Evertide and Volcanite amulet behavior and recipes | ProjectE `EvertideAmulet.java`, `VolcaniteAmulet.java`, and generated recipes | `15d4ce65bd06eb4222709b984255fbf5080e78bc` | MIT | Behavior and recipe-shape research only; Fabric protection tags/callback, exact account transaction, deterministic weather interval, tests, translations, and vanilla-reference models are original ProjectEX work |
| Knowledge Tome behavior and recipe | ProjectE `Tome.java`, `KnowledgeImpl.java`, `SlotConsume.java`, and generated recipe | `15d4ce65bd06eb4222709b984255fbf5080e78bc` | MIT | Full-knowledge concept and recipe-shape research only; explicit server policy, bounded immutable snapshot expansion, atomic saved-data update, tests, translations, and vanilla-reference model are original ProjectEX work |
| Nova and Destruction Catalyst behavior and recipes | ProjectE `DestructionCatalyst.java`, Nova Catalyst content, and generated recipes | `15d4ce65bd06eb4222709b984255fbf5080e78bc` | MIT | Charge/depth, per-block 8 EMC, hardness threshold, names, and recipe-shape research only; bounded geometry, no-explosion policy, protection tags/callback, block-entity exclusion, transactional refund, tests, translations, and vanilla-reference models are original ProjectEX work |
| Body, Soul, and Life Stone behavior and recipes | ProjectE `BodyStone.java`, `SoulStone.java`, `LifeStone.java`, and generated recipes | `15d4ce65bd06eb4222709b984255fbf5080e78bc` | MIT | Hunger/healing roles, 64 EMC unit cost, pedestal concepts, names, and recipe-shape research only; atomic account payment, deterministic player cap, loaded-chunk policy, tests, translations, and vanilla-reference models are original ProjectEX work |
| Klein Star names and tier progression | ProjectE Klein Star content/recipes | `15d4ce65bd06eb4222709b984255fbf5080e78bc` | MIT | Behavior and naming reference only; storage API, codecs, recipe implementation, tests, and vanilla texture references are original ProjectEX work |
| Collector/relay tier names, rates, and capacities | ProjectE `EnumCollectorTier` / `EnumRelayTier` | `15d4ce65bd06eb4222709b984255fbf5080e78bc` | MIT | Balance reference only; fixed-point arithmetic, persistence, access policy, budget, and cycle-safe routing code are original ProjectEX work |
| Condenser target/output and color-keyed bag concepts | ProjectE `CondenserBlockEntity` / `AlchemicalBag` | `15d4ce65bd06eb4222709b984255fbf5080e78bc` | MIT | Behavior research only; exact evaluator, component identity, ownership, nesting policy, and tests are original ProjectEX work |
| Condenser/chest/bag generated models | ProjectEX datagen with vanilla texture identifiers | N/A | MIT / Minecraft EULA | Original JSON layout using runtime-provided vanilla textures; no ProjectE art imported |
| Condenser MK3 capacity and recipe concepts | ProjectExpansion `BlockEntityCondenserMK3` / generated recipe | `168bcf2491b9fde679295fd412ad9c93fd3d93f1` | MIT | Behavior research only; bounded shared-inventory evaluator, paging, atomic accounting, assets, and tests are original ProjectEX work |
| Advanced Alchemical Chest concept | ProjectExpansion `BlockEntityAdvancedAlchemicalChest` / generated recipes | `168bcf2491b9fde679295fd412ad9c93fd3d93f1` | MIT | Behavior research only; ProjectEX uses an original 243-slot owned inventory, component-preserving smithing migration, vanilla-texture model, and Fabric transfer tests |
| Arcane Tablet and knowledge-sharing concepts | ProjectExpansion portable transmutation content | `168bcf2491b9fde679295fd412ad9c93fd3d93f1` | MIT | Concept research only; canonical signed snapshot format, replay guard, limits, confirmation protocol, and tests are original ProjectEX work |
| Dark/red matter tier and furnace balance concepts | ProjectE `EnumMatterType` / `DMFurnaceBlockEntity` | `15d4ce65bd06eb4222709b984255fbf5080e78bc` | MIT | Behavior research only; bounded planning, exact furnace evaluator, armor policy, audit model, and tests are original ProjectEX work |
| Dark/red matter generated models | ProjectEX datagen with vanilla coal/redstone/netherite/furnace texture identifiers | N/A | MIT / Minecraft EULA | Original JSON layout using runtime-provided vanilla textures as explicit placeholders; no ProjectE or ProjectExpansion art imported |
| Expanded fuel/matter naming and Magnum/Colossal/Gargantuan star progression | ProjectExpansion `Fuel`, `Matter`, `Star`, and `ItemStar` | `168bcf2491b9fde679295fd412ad9c93fd3d93f1` | MIT | Naming and balance research only; Fabric registration, arbitrary-precision data schema, atomic reload, recipe logic, tests, and vanilla placeholder references are original ProjectEX work |
| Final Star names and non-depleting food concept | ProjectExpansion `ItemFinalStar`, `ItemFinalStarShard`, and `ItemInfiniteSteak` | `168bcf2491b9fde679295fd412ad9c93fd3d93f1` | MIT | Behavior research only; public Fabric capability, slot resolver, cooldown lease, config validation, atomic EMC payment, server hunger logic, tests, and placeholder models are original ProjectEX work |
| Sixteen expansion machine tiers and power-flower composition | ProjectExpansion `Matter` | `168bcf2491b9fde679295fd412ad9c93fd3d93f1` | MIT | Balance research only; arbitrary-precision catalog, exact fixed-point conversion, documentation, and tests are original ProjectEX work |
| Expansion collector/relay/power-flower/Compact Sun identities and recipes | ProjectExpansion generated recipes and block registrations | `168bcf2491b9fde679295fd412ad9c93fd3d93f1` | MIT | Naming and recipe-shape research only; Fabric blocks, state handling, tick logic, configuration, datagen, tests, translations, and vanilla-reference placeholder models are original ProjectEX work |
| Knowledge Sharing Book identity and recipe shape | ProjectExpansion `ItemKnowledgeSharingBook` and generated recipe | `168bcf2491b9fde679295fd412ad9c93fd3d93f1` | MIT | Identity and recipe-shape research only; signed snapshots, persistent secret/replay/audit data, policy and team boundary, two-phase protocol, confirmation UI, codecs, tests, translations, and placeholder model are original ProjectEX work |
| Four Alchemical Book identities, ratios, capabilities, and recipe shapes | ProjectExpansion `ItemAlchemicalBook`, `CapabilityAlchemicalBookLocations`, and generated recipes | `168bcf2491b9fde679295fd412ad9c93fd3d93f1` | MIT | Progression research only; Fabric item/player persistence, exact-stack session protocol, rate/replay checks, EMC transaction handling, accessible screen, codecs, tests, translations, docs, and placeholder models are original ProjectEX work |

## Rules

- Behavior research does not permit copying without attribution.
- Preserve upstream MIT copyright/license notices for substantial copied portions.
- Never import branding or assets whose ownership/license is unclear.
- Record the immutable upstream commit SHA, not only a moving branch name.
- A reviewer must verify this ledger in every content or asset pull request.

## Release notice audit

The 1.0 documentation audit on 2026-07-19 verified that runtime, sources, and Javadoc
artifacts include `LICENSE_ProjectEX` and `NOTICE_ProjectEX`; ProjectE and
ProjectExpansion references identify immutable commits and their MIT licenses; generated
models use runtime-provided vanilla texture identifiers and contain no copied upstream art.
No additional bundled third-party code, binary, font, sound, or texture requires a separate
notice. `verifyReleaseArtifacts` enforces notice presence on every release build.
