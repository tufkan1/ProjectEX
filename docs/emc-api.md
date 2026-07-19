# Public EMC API

ProjectEX exposes a query-only Java API for Fabric 26.2 consumers. Obtain it through
`ProjectEX.emc()`; never depend on `io.github.tufkan1.projectex.internal` packages.

```java
import io.github.tufkan1.projectex.ProjectEX;
import io.github.tufkan1.projectex.api.emc.EmcKey;
import io.github.tufkan1.projectex.api.emc.EmcValue;

EmcValue diamond = ProjectEX.emc()
    .find(EmcKey.parse("minecraft:diamond"))
    .orElseThrow();
```

Every call to `snapshot()` returns one immutable, internally consistent generation.
Hold that snapshot when performing multiple related reads. A reload replaces the
whole generation atomically.

```java
var subscription = ProjectEX.emc().subscribe(snapshot -> {
    // Runs after publication on the server reload thread. Keep this callback short.
    rebuildMyCache(snapshot);
});

subscription.close();
```

Listener failures are isolated and logged. Callbacks cannot mutate ProjectEX state
because the public contract exposes no mutation method. Registration does not invoke
the listener immediately; read `snapshot()` first when an initial cache is needed.

## Minecraft adapter boundary

The core API uses `EmcKey` so it remains independent of Minecraft mappings.
`MinecraftEmcAdapter` converts an `Identifier`, registered `Item`, or an `ItemStack`
to an item-only match. Component-sensitive values use an exact `EmcMatch` whose
canonical component JSON follows the [EMC data format](emc-data-format.md). API v1
does not guess how a consumer's component variants should fall back.

## Compatibility policy

- `EmcApi.VERSION` is `1` for this contract.
- Additive methods and types may be introduced in compatible releases.
- Removing types, changing method descriptors, or changing lifecycle semantics
  requires a new API contract version and a migration note.
- Packages below `.internal` are implementation details with no binary or source
  compatibility guarantee.
- Minecraft adapters can change alongside Minecraft/Fabric mappings; loader-neutral
  types under `.api.emc` are the long-lived integration surface.

Consumer compile coverage in CI imports only the public entry point and API packages.
Runtime GameTests exercise storage and protection callbacks on a dedicated server. The
repository's [example integration mod](../examples/api-test-mod/README.md) is compiled by
`check`; the [integration guide](integration-guide.md) documents every supported API family.
