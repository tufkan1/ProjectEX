# Knowledge sharing security contract

Knowledge-sharing items use a canonical version-1 snapshot containing a random snapshot
identifier, owner UUID, issue/expiry timestamps, and at most 4,096 sorted unique EMC keys.
The server signs the canonical bytes with HMAC-SHA256; the signing secret is never sent
to clients or stored on the item.

The verification boundary rejects altered signatures, timestamps more than 60 seconds in
the future, expired snapshots, lifetimes over seven days, unsupported versions, and
oversized entry lists before any player state is considered. Confirmed snapshot UUIDs go
through a bounded one-shot replay registry. Preview and confirmation workflows must call
verification again immediately before their atomic merge/replace commit.

This core intentionally does not grant knowledge by itself. Runtime items, confirmation
UI, audit events, server policy, and team adapters are consumers of this fail-closed
format and are implemented as separate server-authoritative layers.
