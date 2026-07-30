# Moonlight Android Architecture

This document defines the target architecture and the rules that apply while the
Axixi-derived application is being converted into an independently maintained
Moonlight client. It is normative: new production code must follow these rules,
and migrated code must satisfy them before its temporary compatibility adapter is
removed.

The ordered migration plan, required evidence, and phase completion gates are
defined in `docs/architecture/REFACTORING_ROADMAP.md`.

## Objectives

- Preserve protocol-visible input, audio, clipboard, controller, and video
  behavior while changing ownership and dependency direction.
- Keep the Android UI lifecycle separate from stream-session and domain state.
- Make every stateful subsystem independently testable without launching
  `Game`.
- Keep latency-sensitive paths deterministic, non-blocking, and allocation
  conscious.
- Stabilize package boundaries before introducing Gradle modules.

Line count reduction is a useful signal but not the goal. A smaller class that
still shares mutable state through callbacks or global registries is not an
architectural improvement.

## Dependency direction

The final dependency direction is:

```text
app composition and Android entry points
    -> feature UI and UI state holders
        -> domain contracts and immutable models
            <- Android and protocol implementations
                -> moonlight-common-c / JNI
```

Dependencies must point toward contracts. Domain contracts must not depend on an
Activity, Fragment, View, dialog, or concrete `NvConnection`.

During the package-first migration, the intended domains are:

- `ui`: Android rendering, navigation, permissions, and lifecycle adapters.
- `binding.input`: input arbitration and device adapters.
- `stream`: stream-session state and resource ownership.
- `render`: viewport, Surface, decoder, FSR, and HDR coordination.
- `settings`: typed configuration and persistence.
- `hosts`: discovery, pairing, certificates, and stored-host data.
- `transfer`: clipboard, file, image, and microphone capabilities.
- `protocol`: narrow protocol ports and their Moonlight implementations.

Physical Gradle modules are introduced only after these package dependencies are
acyclic and stable. This avoids encoding an incorrect architecture into the
build graph.

## State ownership

Each mutable state has exactly one owner:

| State | Owner |
| --- | --- |
| Active contacts, gesture arbitration, press state | Input controller |
| Local cursor reference position | Local cursor controller |
| Connection and stream lifecycle | Stream session controller |
| Window, video rectangle, and coordinate transforms | Viewport controller |
| Surface, decoder, FSR, and HDR resources | Render controller |
| Persistent configuration | Settings repository |
| Controller devices, slots, and reports | Controller subsystem |
| Clipboard and file-transfer jobs | Transfer service |

Other components receive immutable snapshots or explicit callbacks. They must
not mutate another component's fields.

## UI state and events

Non-realtime UI follows one-way state flow:

```text
UI event -> state holder/controller -> immutable UI state -> view rendering
```

Activities and Fragments translate Android callbacks into domain events and
render state. They do not perform protocol I/O, file I/O, or business-state
transitions directly.

The realtime input, audio, and video paths are exceptions to UI state flow.
They use explicit low-overhead ports and state machines described in
`docs/architecture/REALTIME_THREADING.md`.

## Lifecycle and resource ownership

Stateful components expose an explicit lifecycle appropriate to their scope:

```text
create -> start -> active -> stop -> destroy
```

- `stop()` and `destroy()` are idempotent.
- The component that creates a resource closes it.
- A destroyed component cannot post new UI work.
- A late callback from an older generation cannot mutate a newer session.
- Cancellation is a normal terminal path and has dedicated tests.

## Contracts and dependency injection

Constructor injection and small factories are the default. The app entry point
is the composition root.

- Interfaces are defined by the consumer's needs.
- Android components that cannot receive constructor dependencies may use a
  lifecycle-bound registry only as a temporary platform adapter.
- A registry never owns an Activity and must reject stale unregistration.
- Generic service locators, event buses, and mutable static singletons are
  prohibited.
- A dependency-injection framework is introduced only if the stabilized object
  graph demonstrates a concrete need.

## Migration policy

Every migration slice follows this sequence:

1. Add characterization or contract tests for existing behavior.
2. Introduce the narrow contract and lifecycle.
3. Delegate the existing path without changing its algorithm.
4. Move one responsibility at a time.
5. Compare protocol, state, and UI output with the baseline.
6. Delete the old path and temporary adapter within the same phase.

Move-only commits and behavior-changing commits are separate. A phase cannot
finish with two production implementations of the same behavior.

## Verification gates

Every commit must pass:

- relevant JVM or Android component tests;
- compilation of both product flavors;
- architecture dependency tests;
- `git diff --check`.

Every completed phase must also pass:

- `verifyLocal`;
- root and non-root instrumentation tests;
- release assembly and upgrade installation;
- the domain-specific hardware and protocol regression matrix.

Changes to the shared common-c protocol additionally run its CTest suite and
the Android, Qt, and Sunshine protocol fixtures.

## Enforced boundaries

`ArchitectureBoundaryTest` enforces boundaries that are already migrated.
Rules are only added, never weakened to accommodate new violations. Existing
legacy dependencies are recorded as migration work rather than globally
excluded from analysis.
