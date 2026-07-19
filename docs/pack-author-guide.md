# Pack author guide

ProjectEX reads EMC resources from `data/<namespace>/projectex/emc/*.json`. Use your own
namespace so packs do not overwrite one another. A minimal pack is:

```text
my_pack/
  pack.mcmeta
  data/my_pack/projectex/emc/overrides.json
```

```json
{
  "schema_version": 1,
  "priority": 200,
  "values": [
    { "item": "minecraft:diamond", "emc": "9000" },
    { "item": "minecraft:charcoal", "alias": "minecraft:coal" },
    { "item": "example:creative_item", "remove": true }
  ]
}
```

EMC is a canonical non-negative integer string, not a JSON number. Each entry has exactly
one of `emc`, `alias`, or `remove: true`. Component-sensitive entries add an exact
`components` object; item-only definitions do not match arbitrary component variants.
Validate files against [`emc-values.schema.json`](schema/emc-values.schema.json).

## Precedence and conflict diagnostics

Higher priority wins in `-10000..10000`. Give intentional compatibility overrides a
documented priority; do not use an extreme value merely to defeat other packs. At the same
highest priority, equivalent semantics may coalesce, while different definitions are fatal.
Aliases resolve after winners; missing targets and cycles are fatal. Any failure keeps the
entire previous snapshot.

Test in a clean instance and the real pack order. Run `/reload`, `/projectex datapack audit`,
and `/projectex emc <item>` for every important winner. The query reports winning provenance.
Logs identify parse, registry, conflict, and alias failures; `/projectex dump` emits a bounded
registry diagnostic.

## Release checklist

- Include `pack.mcmeta`, schema-v1 resources, license, changelog, and intended version range.
- Test new and upgraded world copies; avoid EMC loops from cheaper reversible recipes.
- Never edit the ProjectEX jar or generated built-in pack; ship a separate data pack.
- Treat recipes and explicit values as server data: clients never select prices.
- Re-run conflict diagnostics whenever adding or removing a mod or pack.

The normative contract remains [EMC data-pack format](emc-data-format.md).
