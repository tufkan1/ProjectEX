# Optional integrations

ProjectEX never requires a recipe viewer or inspection overlay. Optional entrypoints live
under `io.github.tufkan1.projectex.compat`, are not resolved when their host mod is absent,
and cannot calculate authoritative EMC from client resources.

## Compatibility matrix

| Profile | Minecraft | Tested version | Status |
| --- | --- | --- | --- |
| No optional mods | 26.2 | ProjectEX CI | Supported and required |
| Jade | 26.2 | `26.2.9+fabric` | Supported |
| Mod Menu | 26.2 | `20.0.1` | Supported settings entrypoint |
| EMI | 26.2 | No upstream Fabric 26.2 artifact | Waiting for upstream |
| WTHIT | 26.2 | No upstream Fabric 26.2 artifact | Not maintained |

Jade shows collector/relay/power-flower tier and rate. Stored EMC, capacity, redstone mode,
and condenser buffer are sent by the server only when the looking player can use that block.
Automation tooltips send only public block family and tier. They never send account balance,
knowledge, owner UUID, member list, filters, inventories, condenser target, or signed state.
Removing Jade requires no ProjectEX configuration or save migration.

Mod Menu adds a discoverable ProjectEX settings button for the four client-only preferences.
The screen uses vanilla widgets and ProjectEX remains fully functional when Mod Menu is not
installed. Removing it does not delete or reset `client.properties`.

The CI client matrix boots ProjectEX without optional mods and with each pinned integration.
The Jade profile additionally requires discovery of `ProjectEXJadePlugin` and rejects Jade's
plugin-error marker; the Mod Menu profile rejects entrypoint failures. Pins are updated only
after the client profiles, dedicated server suite, and privacy allowlist tests pass.

## EMI status and policy

As of 2026-07-19, EMI's official releases and Maven metadata provide no Minecraft 26.2
Fabric artifact; its newest maintained Minecraft line is 1.21.1. ProjectEX does not compile
against an older Minecraft jar, bundle a private fork, or use reflection to pretend recipe
categories are compatible. The accepted EMI category work remains tracked in issue #45 and
will add transformation, collector-upgrade, and machine categories only after an official
26.2 API can be compiled and exercised in the same present/absent CI matrix.

Version evidence and update sources are the official
[EMI repository](https://github.com/emilyploszaj/emi),
[Jade repository](https://github.com/Snownee/Jade), and their published Modrinth artifacts.
Optional jars are compile/test inputs only and are never nested in ProjectEX artifacts.
