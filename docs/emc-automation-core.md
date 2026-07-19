# EMC automation core

ProjectEX's EMC Link and Transmutation Interface services are server-authoritative,
loader-neutral transaction cores. World blocks and storage-mod adapters must use these
services instead of mutating player EMC directly.

## Identity and access

- Every automation block persists one owner UUID and may persist up to 64 member UUIDs.
- Machine execution uses an explicit machine-binding authority. It never constructs an
  offline player or borrows an online player's permissions.
- Only the online owner or a server-verified operator can edit access lists.
- Public access, when enabled, permits EMC insertion only. It never grants extraction,
  crafting, or knowledge queries.
- Adapters may create machine-binding authority only for the account UUID stored by that
  same block entity. Treating an arbitrary UUID as a machine binding is a security bug.

Access state uses strict schema version 1 through `AutomationAccessCodec`. Invalid UUIDs,
duplicate members, unsupported versions, and malformed flags fail closed.

## EMC Links

`EmcLinkService` supports exact insert and extract requests with independent allow/deny
filters. Filters and request sizes are bounded by the machine tier. Every request carries
a unique identifier and the current server tick; replays and decreasing tick values are
rejected.

The service updates the explicitly bound account with compare-and-set retries. Multiple
links sharing an account therefore cannot overspend it. Adapters must pass the server tick,
not a client-provided value, and expose the service through sided Fabric Transfer API views.

## Transmutation Interface

Knowledge queries accept a bounded candidate set and return only its intersection with the
bound account's learned items. This avoids exposing the complete knowledge index. An
unauthorized query always returns an empty result.

Crafting prices come from an immutable server EMC snapshot. Requests pin its revision, so a
datapack reload cannot mix old and new prices. The `CraftingTransactionTarget` joins the EMC
debit and external item insertion behind one atomic commit boundary:

- `COMMITTED`: both the account replacement and output insertion happened;
- `CONTENTION`: neither happened and the service may retry;
- `OUTPUT_REJECTED`: neither happened because the destination cannot accept the output.

An adapter that inserts an item before committing EMC, performs a compensating refund, or
returns success after only one side commits violates this contract. Fabric storage adapters
should implement the boundary with their transaction context and a revisioned account lock.

## Budgets and audit

Each block instance has tier-derived limits for requests per tick, EMC per request, EMC per
tick, and filter/query size. Request identifiers remain reserved even when a later business
check fails, preventing retry amplification in the same tick.

Every accepted or rejected operation emits `AutomationAuditEvent`, including actor identity
when present, operation, item, requested/transferred EMC, balances, and a machine-readable
failure. Production sinks should rate-limit log rendering while retaining aggregate counters.

## Adapter verification checklist

- Exercise every side and insert/extract permission through Fabric Transfer API.
- Restart the server and verify owner, members, filters, tier, and account binding.
- Race two storage clients against the same balance and assert no negative balance or extra
  output.
- Reload EMC data during a pinned request and assert `STALE_EMC_REVISION`.
- Disconnect the owner and verify machine operation uses only its persisted binding.
- Query as a stranger and assert no knowledge identifiers are returned.
- Fill the output inventory and assert neither EMC nor items change.

The unit suite covers access, persistence corruption, replay/tick limits, filtered transfers,
unauthorized knowledge, rejected destinations, and concurrent overspend. World adapters must
add GameTests and optional storage-mod compatibility fixtures before issue #42 is complete.
