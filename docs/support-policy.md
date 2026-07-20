# Version and support policy

## Minecraft versions

The first supported line is Minecraft Java Edition 26.2 with Fabric. Minecraft
feature releases are not assumed binary compatible. The project ports forward to
26.3 and later through an explicit compatibility issue and test cycle.

The `26.3` prerelease tag covers four isolated targets: `26.3-snapshot-1` through
`26.3-snapshot-4`. Each target has its own Fabric API profile, metadata constraint,
runtime test pass, and game-version-bearing artifact name.

- `main`: next ProjectEX release for the currently documented Minecraft target.
- `x.y`: optional maintenance branch only when a supported line needs fixes.
- Release jar names and GitHub releases include both mod and Minecraft versions.
- A version is supported while it receives security and critical save-corruption fixes.
- End of support is announced in the changelog and README before branch archival.

## Stability expectations

ProjectEX 1.0 freezes the supported public API, configuration schema 1, and persisted-data
format 1 for the Minecraft 26.2 support line. Compatible changes preserve those contracts;
breaking changes require a new major mod version and a documented migration path. Any
release capable of writing world or player state includes a data version and migration
path. Users must still back up worlds before upgrading, and development builds are never
declared world-safe.

## Support scope

Reports must reproduce on Fabric Loader with Fabric API and a minimal mod list.
Conflicts are investigated when the reporter supplies logs and a reproducible case.
Cracked launchers, redistributed jars, modified binaries, and unsupported Minecraft
versions are outside the support scope.
