# Content registration and datagen

ProjectEX treats registry identifiers and generated resources as public compatibility
contracts. Once released, an identifier must not be renamed without a documented data
migration and a compatibility alias where Minecraft supports one.

## Registration

`ProjectEXContentRegistry` owns typed registration helpers. A block family registers
its block and block item under one identifier and returns a `RegisteredBlock` record.
Runtime code and datagen both use that record, avoiding duplicated string identifiers.

`ProjectEXBlocks.register()` only attaches already-registered content to the stable
vanilla Functional Blocks creative tab. New families should follow the same split:
static registry construction first, event wiring in `register()`.

## Providers and ownership

`ProjectEXDataGenerator` is the `fabric-datagen` entrypoint and registers providers for:

- crafting recipes and their unlock advancements;
- block mining tags;
- block loot tables;
- English language output from `src/main/datagen/lang/en_us.json`;
- ProjectEX progression advancements;
- block states, block models, and Minecraft 26.2 client item models.

Generated output lives in `src/main/generated` and is packaged by Loom. Files in that
directory are review artifacts and must be committed, but never edited directly.
Turkish translations remain hand-authored until their provider is introduced; language
key parity is still enforced by tests.

## Workflow

Run the generator after changing a content family, provider, or English source template:

```text
./gradlew runDatagen
```

Then run the complete validation:

```text
./gradlew check build
```

CI runs datagen from a clean checkout and fails if tracked output changes or a new
untracked generated file appears. This makes missing resources and non-deterministic
providers visible in the pull request.

Every imported or adapted source or asset must also update `docs/provenance.md` with an
immutable upstream commit. The current generated Transmutation Table family is original
ProjectEX work and does not copy ProjectE or ProjectExpansion code or assets.
