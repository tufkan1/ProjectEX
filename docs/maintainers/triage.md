# Issue and pull request triage

## New issues

Within seven days, verify support scope, reproduction/reference quality, security
sensitivity, duplicates, and milestone relevance. Keep `status: needs-triage` until a
maintainer either accepts scope, asks for information, marks a duplicate, or closes as
not planned. Add exactly one type, one priority, one or more area labels, and a milestone
only when scheduled.

Critical means security, data corruption/loss, reliable duplication, startup crash for
a supported setup, or a release blocker. Move exploit details to a private advisory and
remove sensitive public details where GitHub permits.

## Accepted work

An accepted issue states observable behavior, exclusions, dependencies, test plan,
save/network impact, provenance expectations, and acceptance criteria. Add `help wanted`
only when maintainers can review an external solution; add `good first issue` only when
no hidden architectural decision remains.

## Pull requests

Link an accepted issue, confirm CI and checklist, review server authority, persistence,
resource generation, licensing, and user-visible documentation. Prefer requested changes
for correctness/security gaps and follow-up issues only for safely separable improvements.

## Stale items

Do not auto-close confirmed bugs. For incomplete reports, request specific information
and allow at least 30 days. For inactive claimed tasks, ask before unassigning so another
contributor can continue. Preserve useful design history when closing.
