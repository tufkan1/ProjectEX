# Player guide

## Install and update

ProjectEX 1.0 targets Minecraft Java Edition 26.2, Java 25, Fabric Loader 0.19.3 or
newer, and Fabric API 0.155.2+26.2. Install the matching Fabric Loader profile, place
Fabric API and the ProjectEX runtime jar in the instance's `mods` directory, and launch
once. Client and server must use the same ProjectEX version. Never replace a jar while a
world is running; back up the world before any update.

ProjectEX is server-authoritative. It is required on both sides for normal play, but
changing `client.properties` affects only local preferences. Development builds and jars
for a different Minecraft feature release are not save-compatibility promises.

## First progression

1. Craft a Philosopher's Stone and Transmutation Table using the recipe book.
2. Right-click the table. Learn a held EMC-valued item, then burn valued items to fund the
   account. Learned items appear in the searchable browser.
3. Create a learned item only when the account has its current server EMC price. Favorites,
   pages, keyboard focus, and search do not change server state.
4. Craft Klein Stars for portable EMC. Collectors generate EMC, relays accept and route it,
   and adjacent networks charge stars without loading chunks.
5. Advance through Dark and Red Matter, then the linear expansion chain: Magenta, Pink,
   Purple, Violet, Blue, Cyan, Green, Lime, Yellow, Orange, White, Fading, and Final.
6. EMC Links expose an owner's balance to automation; Power Flowers combine generation and
   routing. Condensers convert supplied EMC-bearing inputs into an exact target item.

Recipe packs may change costs and shapes, so the in-game recipe book and `/projectex emc
<namespace:item>` are authoritative. See [expanded progression](expanded-material-progression.md)
and [machine balance](expansion-machine-balance.md) for exact formulas.

## Controls and accessibility

Right-click opens tables, portable tablets, storage, and books. ProjectEX screens support
keyboard focus, Tab/Shift+Tab traversal, Enter/Space activation, Escape to close, typed
search, paging, and narrated labels/status. Search and page selection remain local until a
server action is confirmed. Knowledge sharing and teleport operations show a preview or an
explicit destination before committing.

The defaults are `V` for charge level, `G` for mode, and `K` to open the first Arcane
Tablet found in the player's inventory. They are ordinary translated key mappings under
**Settings > Controls > Key Binds > ProjectEX**, so every binding can be changed or cleared.
Tablet opening is validated by the server; the shortcut cannot open an item the player no
longer carries. ProjectEX conveys state with text and narration in addition to color. Reduce
rapid repeated input if a server rate-limit message appears.

With the optional Mod Menu installed, **Mods > ProjectEX > Configure** exposes local-only
switches for EMC tooltips, compact EMC numbers, remembered transmutation favorites, and
automatic search focus. The same values are persisted in `config/projectex/client.properties`;
they never modify server rules.

## Machine basics

- Collectors generate exact fixed-point EMC and upgrade valid fuel in their inventory.
- Relays move EMC through loaded adjacent networks. Cycles are detected and work is bounded.
- Power Flowers combine tier-matched collectors and relays; a Compact Sun below one applies
  the configured multiplier.
- Condensers require a target. Output commits only when the complete item fits.
- EMC Links and the Transmutation Interface use owner/member access, side rules, filters,
  request budgets, and exact rollback. Public insert never implies public extraction.
- Alchemical Chests and Bags preserve components; bags cannot nest recursively.

## Troubleshooting

- **Missing EMC:** ask an operator to run `/projectex emc <item>` and `/projectex datapack
  audit`. A pack may remove the value or a conflict may have preserved the old snapshot.
- **Machine stopped:** verify loaded chunks, redstone/access modes, target/output space,
  valid fuel, and adjacency. Operators can use `/projectex machine audit <x y z>`.
- **Action rejected:** reopen the screen for a fresh server session. Price, inventory,
  distance, permissions, or replay/rate limits may have changed.
- **Join/version error:** match Minecraft, Loader, Fabric API, and ProjectEX on both sides.
- **Recovery warning:** stop making changes and contact the operator; do not delete ProjectEX
  world data. The [server guide](server-guide.md) contains recovery steps.

Report reproducible problems using the repository issue forms. Include versions, relevant
logs, pack list, and a minimal reproduction; never attach secrets.
