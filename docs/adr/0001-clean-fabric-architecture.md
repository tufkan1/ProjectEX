# ADR 0001: Clean Fabric architecture instead of mechanical NeoForge translation

- Status: accepted
- Date: 2026-07-19

## Context

ProjectE and ProjectExpansion target Forge/NeoForge-era APIs and together contain
hundreds of Java classes. Minecraft 26.x is unobfuscated and Fabric 26.2 uses a new
Loom plugin/toolchain. Capability, event bus, registration, networking, attachment,
rendering, and configuration patterns do not map one-to-one to Fabric.

## Decision

Reimplement behavior in vertical slices behind loader-neutral domain services. Use
Fabric APIs and vanilla facilities first. Treat upstream code as a behavior and test
reference; adapt code only when it is clearer and record it in the provenance ledger.

## Consequences

Initial visible feature delivery is slower, but each slice remains testable, forward-
portable, and auditable. “Parity” is tracked as behavior, not identical class layout.
