# EMC data-pack format

ProjectEX reads versioned JSON resources from
`data/<namespace>/projectex/emc/*.json`. Schema version 1 is defined by
[`docs/schema/emc-values.schema.json`](schema/emc-values.schema.json).

```json
{
  "schema_version": 1,
  "priority": 100,
  "values": [
    { "item": "minecraft:coal", "emc": "128" },
    { "item": "minecraft:charcoal", "alias": "minecraft:coal" },
    { "item": "example:disabled_item", "remove": true },
    {
      "item": "example:charged_crystal",
      "components": { "example:charge": 100 },
      "emc": "8192"
    }
  ]
}
```

## Rules

- `schema_version` is required and currently must be `1`.
- `priority` defaults to `0` and is limited to `-10000..10000`.
- Each entry contains `item` and exactly one operation: `emc`, `alias`, or
  `remove: true`.
- EMC is a canonical non-negative integer **string** of at most 1000 digits.
- `components`, when present, is an exact component constraint. JSON object key order
  does not change its identity. Component semantics are validated against registries
  during server reload.
- Duplicate item/component matches in one file are rejected.
- Unknown fields and unknown schema versions are rejected instead of ignored.

## Conflict resolution contract

Definitions from all resources are grouped by item/component match. Higher `priority`
wins. Conflicting definitions with the same highest priority are a fatal reload error;
identical definitions may coalesce. This avoids silently depending on mod registration,
filesystem, or resource enumeration order. A removal participates in the same priority
rules. Aliases resolve after conflicts, may target explicit or mapped values, and alias
cycles are fatal.

If parsing, registry validation, conflict resolution, or alias resolution fails, the
entire reload fails and the last valid immutable EMC snapshot remains active.
