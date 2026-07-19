# ProjectEX API example mod

This is the smallest compile-checked consumer of the supported public API. The root
`compileApiExample` task builds it, an import audit rejects implementation dependencies, and
`check` assembles a test-mod jar under `build/examples`. Copy the class and `fabric.mod.json`
into a normal Fabric Loom 26.2 mod,
replace the example id/package, and add the dependency from the
[integration guide](../../docs/integration-guide.md).

The example reads an immutable EMC snapshot, subscribes to atomic publications, and registers
a protection callback. A real mod closes the subscription with its owning lifecycle and keeps
callbacks short.
