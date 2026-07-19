# Source and asset provenance

This file is the auditable ledger for adapted code and assets. Add one row in the
same pull request that introduces any derived material.

| ProjectEX path | Origin project/path | Origin commit | License | Adaptation notes |
| --- | --- | --- | --- | --- |
| `src/main/generated/**` | ProjectEX datagen providers | N/A | MIT | Original ProjectEX data/models; no upstream code or assets copied |
| Core material names and recipe progression | ProjectE content/recipes | `15d4ce65bd06eb4222709b984255fbf5080e78bc` | MIT | Behavior and compatibility research; original Fabric code and vanilla texture references |
| `PhilosophersStoneItem` behavior | ProjectE `PhilosophersStone.java` | `15d4ce65bd06eb4222709b984255fbf5080e78bc` | MIT | Mode/charge concepts researched; transaction, protection, component, and interaction code are original |
| Klein Star names and tier progression | ProjectE Klein Star content/recipes | `15d4ce65bd06eb4222709b984255fbf5080e78bc` | MIT | Behavior and naming reference only; storage API, codecs, recipe implementation, tests, and vanilla texture references are original ProjectEX work |
| Collector/relay tier names, rates, and capacities | ProjectE `EnumCollectorTier` / `EnumRelayTier` | `15d4ce65bd06eb4222709b984255fbf5080e78bc` | MIT | Balance reference only; fixed-point arithmetic, persistence, access policy, budget, and cycle-safe routing code are original ProjectEX work |

## Rules

- Behavior research does not permit copying without attribution.
- Preserve upstream MIT copyright/license notices for substantial copied portions.
- Never import branding or assets whose ownership/license is unclear.
- Record the immutable upstream commit SHA, not only a moving branch name.
- A reviewer must verify this ledger in every content or asset pull request.
