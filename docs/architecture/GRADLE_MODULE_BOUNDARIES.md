# Gradle module boundaries

This document records the measured dependency evidence and ownership rules for
physical Android module extraction. A module is introduced only when the build
can enforce a stable architectural boundary; package count alone is not a
reason to split code.

## Baseline

Before the first extraction, all 595 production Java files were compiled in
the `app` Android application module. Import analysis identified two mature,
platform-independent domains:

| Domain | Pure Java files | Android adapter files | External dependency |
| --- | ---: | ---: | --- |
| Host identity, repository, pairing, and reachability policy | 30 | 17 | None |
| Typed settings and runtime settings state | 68 | 13 | Gson |
| File-manifest protocol and transfer lifecycle | 2 | 4 | None |
| Virtual-control layout documents and repository contract | 8 | 1 | None |

The only dependency leaving the pure settings domain is from virtual-control
settings to the virtual-control layout model. The host and transfer cores are
independent of both. None of these domains imports Android, AndroidX, an
Activity, a View, `SharedPreferences`, JNI, NvHTTP, or a transport DTO.

## Enforced graph

```text
app
 |---> core:hosts
 |---> core:settings ---> core:virtual-controls
 |---> core:transfer
 +---> core:virtual-controls
```

- `core:hosts` owns immutable host identity/runtime values, repository and
  discovery contracts, pairing policy, bounded reachability selection, and
  host quit/unpair use cases. Android database, mDNS, and NvHTTP adapters stay
  in `app`.
- `core:virtual-controls` owns immutable layout values, document encoding,
  repository contracts, and their JVM tests.
- `core:transfer` owns the platform-neutral file-manifest wire model,
  validation and encoding plus transfer lifecycle state. Android source URIs,
  SAF enumeration/materialization, NvHTTP transport, and progress UI stay in
  `app`.
- `core:settings` owns typed keys, migrations, immutable settings snapshots,
  codecs, update transactions, runtime state, and their JVM tests. It exports
  `core:virtual-controls` because virtual-control layout types occur in its
  public settings contract.
- `app` owns Android persistence adapters, platform defaults, UI, composition,
  native transport, and packaging. It may depend on both core modules; neither
  core module can depend on `app` because Gradle has no reverse project edge
  and their Java compilation classpaths contain no Android SDK.

## Verification ownership

- Each core module runs its own unit tests through its `test` task.
- Root `nonRootRelease` is the default daily build and assembles only the
  ordinary non-root Release APK. `verifyNonRootRelease` is the corresponding
  single-variant quality gate.
- Root `verifyLocal` depends explicitly on every core test task in addition to
  every app JVM variant, all four app Lint variants, and root/non-root Release
  assembly.
- Android instrumentation remains in `app`, where Android adapters are
  composed with core contracts.
- The transfer module tests its encoder and validator directly against the
  canonical common-c manifest fixture reached through the checked-in native
  revision. The fixture remains authored in common-c rather than copied into
  the Android source tree.

## Build governance

- The Gradle 9.6.1 wrapper is pinned with its official SHA-256 checksum. Android
  Gradle Plugin 9.3.1, all application libraries, and test libraries are owned
  by the version catalog rather than repeated in module scripts.
- `build-logic` owns the pure-Java library convention and root verification
  rules. Core modules declare only their actual dependencies; Java level and
  compiler encoding are consistent by construction.
- `settings.gradle` is the only repository owner and rejects repositories added
  by projects. Dependency verification requires checked-in SHA-256 values for
  every resolved plugin and library artifact.
- Local build cache and configuration cache are enabled. A warm full
  `verifyLocal` gate completes in 12 seconds on the reference workstation after
  module extraction and build modernization.
- A repository-owned build service holds an OS-level worktree lock for the
  lifetime of each Gradle invocation. Independent invocations wait rather than
  concurrently rewriting AGP intermediates; tasks within one invocation stay
  parallel-capable.
- The app compiles and targets API 37 with API 23 as its minimum. This explicit
  Android 6.0 baseline allows current Activity, Core, Compose, and Material 3
  releases instead of maintaining an obsolete UI dependency branch.
- AGP 9.3.1's `BidiSpoofing` detector is isolated because it crashes before
  reporting. The mandatory cacheable `verifyNoBidiControls` task replaces it
  with a source-wide scan; all other lint findings remain warnings-as-errors.
  Kotlin/UAST lint analysis is executed with JDK 21.
- Android release variants remain unobfuscated by product policy. This build
  governance work does not add R8 or ProGuard processing.

## Next measured candidates

Future extraction must be based on the same evidence. No additional Java
domain currently meets the extraction threshold. Input, rendering, stream
session, and the actual file movers still have substantial Android/native or
transport coupling and must not be split until their adapter seams are
complete.
