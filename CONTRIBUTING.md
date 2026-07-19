# Contributing to ProjectEX

Thank you for helping build ProjectEX. Start with a GitHub issue before large changes
so design, scope, licensing, and save compatibility can be agreed early.

## Development setup

1. Install a 64-bit JDK 25 and clone the repository.
2. Run `./gradlew build` (`.\gradlew.bat build` on Windows).
3. Import the Gradle project into IntelliJ IDEA or another Java IDE.
4. Use `./gradlew runClient` only after the build and unit tests pass.

Do not commit IDE files, `run/`, generated build output, downloaded mods, logs, or
world saves. Never include secrets in configuration, fixtures, or screenshots.

## Workflow

1. Choose an accepted issue and comment that you are working on it.
2. Branch from current `main` using `feature/<issue>-short-name` or
   `fix/<issue>-short-name`.
3. Keep commits focused and written in imperative form.
4. Add tests, docs, translations, changelog, and provenance with the implementation.
5. Run `./gradlew check build` and complete the pull request template.

PRs should normally solve one issue. Draft PRs are welcome for early architectural
feedback. Maintainers may close inactive work after confirming it can be resumed by
another contributor.

## Code rules

- Java 25 language level; four-space indentation and UTF-8.
- No client classes in common/server initialization paths.
- No client-provided EMC values or unvalidated state mutations.
- Prefer immutable values, explicit failure results, and deterministic ordering.
- Public APIs need Javadoc and compatibility consideration.
- A mixin needs a short justification, narrow target, and regression test.
- Persistent or network formats require a version and migration/compatibility plan.

## Tests

Pure domain behavior receives unit tests. Minecraft integration receives GameTests or
integration fixtures. Blocks and menus require dedicated-server coverage. Bug fixes
must add a regression test unless the PR explains why automation is impractical.

## Upstream code and assets

ProjectE and ProjectExpansion are MIT-licensed references, but attribution remains
mandatory. Update `docs/provenance.md`, preserve copyright notices, and describe the
adaptation in the PR. Do not submit an asset unless its license and origin are known.

## Review and releases

At least one maintainer approval and green required checks are expected. Authors do
not merge their own PRs unless a time-sensitive security fix follows the emergency
process in `SECURITY.md`. Releases are produced from signed `v*` tags by automation.
