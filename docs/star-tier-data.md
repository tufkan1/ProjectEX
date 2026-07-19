# Portable star tier data

ProjectEX registers a fixed chain of 24 portable EMC items so item identifiers and saved
inventories remain stable. Their capacities are server data and can be replaced by a data pack.

The winning resource is `data/projectex/projectex/star_tiers.json`. Schema version 1 requires
exactly one entry for every registered tier, in progression order. `capacity` is a canonical
positive integer string (up to 80 digits), so values never pass through floating point or a
64-bit integer.

Every capacity must be strictly greater than the previous capacity. The complete file is parsed
and validated before its immutable snapshot is published. An invalid reload fails closed and
does not partially apply a tier table.

The custom star upgrade recipe accepts exactly four equal-tier stars plus one Aeternalis Fuel.
It creates the next registered tier and copies the exact sum of all four stored-EMC components.
The recipe rejects mixed tiers, corrupt source overflows, and sums above the configured target
capacity. This makes every boundary—including Klein Omega to Magnum Ein—use the same atomic
upgrade rule.
