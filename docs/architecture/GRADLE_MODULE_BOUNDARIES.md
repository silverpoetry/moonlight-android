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
- Root `verifyLocal` depends explicitly on every core test task in addition to
  every app JVM variant, all four app Lint variants, and root/non-root Release
  assembly.
- Android instrumentation remains in `app`, where Android adapters are
  composed with core contracts.
- The transfer module tests its encoder and validator directly against the
  canonical common-c manifest fixture reached through the checked-in native
  revision. The fixture remains authored in common-c rather than copied into
  the Android source tree.

## Next measured candidates

Future extraction must be based on the same evidence. No additional Java
domain currently meets the extraction threshold. Input, rendering, stream
session, and the actual file movers still have substantial Android/native or
transport coupling and must not be split until their adapter seams are
complete.
