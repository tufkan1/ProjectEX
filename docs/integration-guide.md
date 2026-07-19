# Java integration guide

Every GitHub release publishes runtime, sources, Javadoc, and SHA-256 checksum files. Download
and verify the matching runtime jar into your integration mod's `libs` directory. Use
`modCompileOnly` when ProjectEX is optional or `modImplementation` plus a `fabric.mod.json`
dependency when it is required. Do not depend on a source checkout or implementation class.

```groovy
dependencies {
    modCompileOnly files("libs/projectex-fabric-<minecraft>-<mod-version>.jar")
}
```

The supported entry point is `ProjectEX.emc()` and supported types live under
`io.github.tufkan1.projectex.api`. API families are:

- `api.emc` and `api.fabric`: immutable snapshots, exact keys/matches/values, reload
  subscriptions, and Minecraft adapters.
- `api.storage`: Fabric item lookup, server context, query views, and atomic simulate/execute
  transfers for portable EMC containers.
- `api.alchemy`, `api.matter`, `api.teleport`, and `api.utility`: server-side veto events for
  claim/protection integrations. Deny before ProjectEX debits or changes the world.
- `api.endgame`: Final Star lookup and shared-cooldown activation.

Read one snapshot for a multi-read calculation; close subscriptions; keep reload callbacks
short; simulate before execute; never authorize a client request; and do not retain server or
player objects beyond their lifecycle. [`examples/api-test-mod`](../examples/api-test-mod/README.md)
is compiled, import-audited, and assembled into `build/examples` by `./gradlew check`.

`EmcApi.VERSION` identifies the loader-neutral EMC contract. Additive changes retain the
version; descriptor removal, lifecycle changes, or incompatible semantics require a new
version and migration note. Minecraft/Fabric adapter signatures may move at a Minecraft
feature boundary. Everything outside `.api`—especially `.internal`—has no compatibility
guarantee. Generate authoritative HTML with `./gradlew javadoc`; releases ship the same
reference as the `-javadoc.jar` classifier.
