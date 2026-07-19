# 1.0 release audit and regression budgets

Run the complete local gate with:

```text
./gradlew --no-daemon releaseAudit
```

This composes unit/fuzz/concurrency tests, 49 dedicated-server GameTests, the production
client journey, runtime/source JAR creation, and artifact inspection. Pull requests also
run dependency review, CodeQL, a full-history Gitleaks scan, generated-resource checks,
and the same artifact verifier.

## Supported envelope and hard ceilings

The 1.0 support target is 128 persistent player alchemy records and 1,024 active EMC
machines per loaded level. The deterministic soak performs 200 ticks (204,800 exact
transfers), restoring each moved unit so any duplication/loss is visible. Runtime machine
work remains additionally bounded by the server config tick limits.

| Budget | Regression ceiling | 2026-07-19 Windows/Java 25 evidence |
|---|---:|---:|
| Soak CPU | 5,000 ms | 187 ms |
| Soak thread allocation | 512 MiB | 121,872,296 bytes |
| 128-player save (32,768 knowledge entries) | 2 MiB | 551,986 bytes |
| Client action packet | 512 B | 293 B |
| Full 54-entry knowledge page | 72 KiB | 69,358 B |
| Full 64-entry Alchemical Book view | 32 KiB | 16,867 B |
| Runtime/source JAR | 4 MiB each | verifier enforced |
| Public API Javadoc JAR | 8 MiB | verifier enforced |

JUnit publishes the measured values into XML test evidence on every runner. Thresholds
are intentionally above ordinary variance but low enough to fail accidental unbounded
growth. Knowledge-page EMC strings are capped at 1,024 digits and teleport prices at 128
digits before allocation/encoding; player balance packets retain the independent 4,096
digit persistence limit.

## Security and multiplayer campaigns

- Deterministic 10,000-input packet fuzzing never converts malformed identifiers or
  operations into trusted transactions.
- Every supported player is independently limited to 40 wire requests/second; window
  expiry, disconnect cleanup, replay ordering, stale sessions, replacement stacks, and
  simulated latency are covered.
- The privilege matrix proves owners/members/operators/machine bindings retain intended
  access while public automation is insert-only; public extraction, knowledge enumeration,
  and crafting remain denied. Machine and Alchemical Book owner policies have dedicated
  server tests.
- Eight concurrent automation workers perform 8,000 revisioned credits without overspend
  or duplication. Four concurrent readers observe 2,000 EMC reload publications without
  mismatched value/source maps.
- Break/place component round trips cover collectors, relays, condensers, storage,
  automation, bags, furnaces, and pedestal state. Migration GameTests create a SHA-256
  backup and offline crash-recovery package; corrupt player payloads and replay ledgers
  survive codec reload without silent deletion.
- Protection callbacks are evaluated before world mutation or EMC debit for matter tools,
  transmutation, destructive utilities, and teleportation.

## Supply-chain and manual review

The release gate includes wrapper validation, dependency review failing at moderate or
higher severity, weekly/PR CodeQL, weekly/PR full-history secret scanning, and runtime and
sources JAR inspection. The verifier requires `fabric.mod.json`, `LICENSE`, and `NOTICE`,
rejects key/credential/environment/run-directory entries, limits runtime/source artifacts
to 4 MiB and Javadocs to 8 MiB, and requires exactly the three expected JARs.

Manual review on 2026-07-19 traced every client mutation packet to a server-created session,
bounded decoder, authority check, replay/rate guard, and atomic domain commit. Persistence
maps and audit ledgers are bounded or explicitly covered by the published save budget.
No known critical/high finding, item/EMC duplication path, unauthorized mutation, unbounded
packet field, or partial reload publication remains. Optional EMI/overlay compatibility is
tracked separately and is not part of the authoritative mutation surface.
