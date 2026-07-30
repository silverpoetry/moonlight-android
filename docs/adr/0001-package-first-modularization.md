# ADR 0001: Stabilize Package Boundaries Before Gradle Modules

- Status: Accepted
- Date: 2026-07-31

## Context

The application is a single Android module containing inherited Moonlight code
and substantial Axixi-specific input, UI, controller, clipboard, microphone,
and rendering behavior. Several large classes cross these responsibilities.

Creating many Gradle modules immediately would make those existing dependency
cycles build-time dependencies and would add migration boilerplate before the
correct contracts are known.

## Decision

The refactor first creates domain packages, narrow interfaces, immutable models,
and executable dependency rules inside the existing module.

A boundary becomes a Gradle module only after:

- its public contract is stable;
- its package dependency graph is acyclic;
- it can be tested independently;
- moving it provides enforcement, ownership, reuse, or build-time value.

The eventual module graph follows the dependency direction in
`ARCHITECTURE.md`.

## Consequences

- Architecture violations can still compile during early migration, so
  ArchUnit rules are expanded after each boundary is established.
- Package moves remain small and reversible.
- Gradle module creation becomes a mechanical enforcement step rather than an
  architectural experiment.
