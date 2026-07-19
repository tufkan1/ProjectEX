# EMC storage API

ProjectEX exposes portable and machine EMC containers through a versioned Fabric
item lookup. The API is server-authoritative: callers must resolve and transact on
the Minecraft server thread. Clients may render synchronized values, but must not
predict transfers.

## Finding storage

Use a player context for an interactive action or an automation context for a
machine. A missing result means the stack does not expose EMC storage in that
context.

```java
EmcStorageContext context = EmcStorageContext.automation(serverLevel);
Optional<EmcStorage> storage = EmcStorageApi.find(stack, context);

EmcTransferResult result = storage.orElseThrow().insert(
    EmcValue.of(1_000),
    EmcTransferMode.EXECUTE
);
```

`EmcStorageApi.VERSION` is the compatibility version of this contract. Third-party
items can register providers with `EmcStorageApi.LOOKUP.registerForItems(...)`.
Providers must return `null` when their item or context is unsupported.

## Transaction rules

- `SIMULATE` performs the same validation and accounting as `EXECUTE` without
  mutating the stack.
- `requested == transferred + remainder` for every result.
- Partial transfers are valid. Callers must inspect `remainder` or `complete()`.
- A denied transaction transfers zero EMC and reports `allowed == false`.
- Values use the arbitrary-precision `EmcValue` type; narrowing to `long` is not
  part of the API contract.
- Implementations must reject corrupt or overflowing states instead of clamping,
  wrapping, or creating EMC.
- Automation must obey `EmcAutomationPolicy`. The baseline Klein Stars permit
  both insertion and extraction.

## Portable component and migration

Klein Stars store EMC in the versioned `projectex:portable_emc` data component.
Version 1 encodes a non-negative decimal string, preserving exact values across
stack copies, inventory moves, crafting, death drops, save/load, and network
synchronization. The decoder accepts the legacy `{ "emc": "..." }` shape and
migrates it to version 1. Malformed, negative, or over-limit values are rejected.

The component codec has a 4096-decimal-digit safety limit. This limit protects
decoding resources; individual storage capacity remains the stricter transaction
bound.

## Klein Star progression

| Tier | Capacity |
| --- | ---: |
| Ein | 50,000 EMC |
| Zwei | 200,000 EMC |
| Drei | 800,000 EMC |
| Vier | 3,200,000 EMC |
| Sphere | 12,800,000 EMC |
| Omega | 51,200,000 EMC |

An upgrade combines four identical non-Omega Klein Stars with one Aeternalis Fuel.
The custom recipe sums and retains their exact stored EMC. Mixed tiers, unrelated
ingredients, and totals above the output capacity fail without producing output.

The item tooltip and durability-style bar are read-only views of the synchronized
component. They do not initiate or predict a storage transaction.

## Compatibility policy

Additive methods may appear without increasing `VERSION`. Breaking signatures,
accounting semantics, component identity, or context meaning require a new API
version and a documented migration path. Consumer compilation and server GameTests
guard the public surface in CI.
