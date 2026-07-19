# Release playbook

1. Confirm the milestone has no open blocker and CI is green on `main`.
2. Update `gradle.properties`, move changelog entries under the version/date, and verify
   Minecraft/Fabric versions in README and metadata.
3. Run `./gradlew clean check build`; inspect jar contents and test a fresh client plus
   dedicated server. Rehearse upgrade fixtures for any stateful release.
4. Merge the release PR, then create a signed annotated tag matching `v<mod_version>`.
5. Push the tag. `release.yml` rebuilds, creates SHA-256 checksums, and opens the GitHub
   release. Verify its generated notes and mark world-safety/pre-release status clearly.
6. When PX-507 is complete, promote the same checked jar to Modrinth and CurseForge; do
   not rebuild different bytes for each platform.
7. Announce known issues and support line, then open the next changelog section.

Never release from an unreviewed working tree or manually replace an existing release
asset. A bad release is deprecated with an explanation and superseding version.
