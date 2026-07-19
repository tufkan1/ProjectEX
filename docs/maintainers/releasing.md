# Release playbook

1. Confirm the milestone has no open blocker and CI is green on `main`.
2. Update `gradle.properties`, move changelog entries under the version/date, and verify
   Minecraft/Fabric versions in README and metadata.
3. Run `./gradlew clean releaseAudit verifyNoCompatClient`, followed by
   `./gradlew -PcompatJade verifyJadeCompatClient`. Inspect the runtime, sources,
   Javadocs and CycloneDX SBOM. Rehearse upgrade fixtures for any stateful release.
4. Merge the release PR, then create an SSH-signed annotated tag matching
   `v<mod_version>` with a key listed in `.github/release-signers`.
5. Push the tag. `release.yml` cryptographically rejects unsigned, unauthorized,
   off-main, or version-mismatched tags; runs both
   optional-mod profiles, rebuilds the jars byte-for-byte, emits SHA-256 checksums and
   a CycloneDX 1.6 SBOM, creates GitHub provenance/SBOM attestations, and publishes the
   curated release notes.
6. Verify the downloaded runtime with `sha256sum --check SHA256SUMS` and
   `gh attestation verify <jar> --repo tufkan1/ProjectEX`.
7. To promote those exact GitHub bytes, configure the `distribution` environment with
   `MODRINTH_PROJECT_ID`, `MODRINTH_TOKEN`, `CURSEFORGE_PROJECT_ID`, and
   `CURSEFORGE_TOKEN`, then manually dispatch `publish-platforms.yml` with the tag.
   The job downloads, checksums and verifies provenance before publishing; it never
   rebuilds platform-specific jars.
8. Announce known issues and support line, then open the next changelog section.

Never release from an unreviewed working tree or manually replace an existing release
asset. A bad release is deprecated with an explanation and superseding version.
