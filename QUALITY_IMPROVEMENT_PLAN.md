# Android Quality Improvement Plan

This plan is the release checklist for removing the project's reviewed Android Lint
debt without mixing unrelated compatibility, dependency, UI, and resource changes.
Each phase must be independently reviewable, testable, and revertible. A phase does
not advance until its exit gate passes.

## Invariants

- Preserve both `root` and `nonRoot` product flavors.
- Preserve existing application IDs, signing, upgrade compatibility, and user data.
- Keep Release builds unminified.
- Keep `warningsAsErrors` enabled.
- Do not regenerate `lint-baseline.xml` just to make Lint pass. Remove entries only
  after the corresponding finding is fixed, or replace an intentional finding with
  the narrowest documented suppression.
- Do not combine target SDK migration, major dependency upgrades, and visual resource
  changes in one commit.
- Run the shared `moonlight-common-c` CTest suite if that submodule changes.

## Baseline

The initial reviewed Lint baseline contains 67 findings:

| Lint ID | Count | Disposition |
| --- | ---: | --- |
| `UnusedAttribute` | 31 | Review as intentional API-level compatibility; use versioned resources or narrow documentation where appropriate. |
| `VectorPath` | 19 | Profile and simplify only paths with measurable rendering value. |
| `GradleDependency` | 6 | Upgrade one dependency family at a time with feature-specific regression tests. |
| `SourceLockedOrientationActivity` | 3 | Replace assumptions about fixed orientation with adaptive stream and settings layouts. |
| `Overdraw` | 2 | Remove only backgrounds proven to be redundant. |
| `VectorRaster` | 2 | Rasterize only assets whose large intrinsic vector size is actually inappropriate. |
| `DiscouragedApi` | 1 | Resolve with the orientation work above. |
| `IconLauncherShape` | 1 | Move the launcher branding to a proper adaptive icon. |
| `IconMissingDensityFolder` | 1 | Confirm vector/`nodpi` use and document or restructure resources without duplicating assets. |
| `OldTargetApi` | 1 | Migrate `compileSdk` and `targetSdk` to API 36 in isolated steps. |

`MissingTranslation` remains a separately documented project policy for incomplete
community translations and is not part of the 67-entry baseline.

## Phase 1: Reproducible Baseline

- Record the current commit, build toolchain, Lint inventory, Release APK hashes, and
  local/connected test results.
- Verify all JVM tests, every Lint variant, and both Release flavors.
- Verify every instrumentation test on exactly one configured device.
- Maintain a functional regression matrix covering:
  - discovery, pairing, stored certificates, and reconnect;
  - stream start/stop, picture-in-picture, rotation, and window changes;
  - mouse, touchpad, and touchscreen input modes;
  - two-finger right click, three-finger keyboard interception, long/force press,
    local cursor position, keyboard, and controllers;
  - microphone transport;
  - bidirectional text, image, file, and directory clipboard transfer, including
    lazy transfer of large directory trees without blocking input;
  - return menu, shortcuts, and customizable first-page cards.

### Exit gate

`verifyLocal` and `verifyConnected` pass, or any unavailable hardware-only checks are
explicitly recorded without claiming they passed.

## Phase 2: API 36 Migration

Use separate commits so target-gated behavior changes can be isolated:

1. Upgrade the Android build toolchain and `compileSdk` to 36 while retaining
   `targetSdk 34`.
2. Set `targetSdk 35`, fix and verify Android 15 target behavior.
3. Set `targetSdk 36`, fix and verify Android 16 target behavior.

The migration must explicitly cover edge-to-edge layout, supported predictive-back
callbacks, large-screen orientation/resizability behavior, local-network discovery and
streaming, microphone/audio focus, share intents, and storage access. The stream video
rectangle, local cursor coordinates, and touch mappings must be recalculated from the
same window geometry after any inset or windowing change.

### Exit gate

- API 21/28 compatibility smoke tests.
- API 34 comparison tests.
- API 35 and 36 lifecycle/layout tests.
- Real-device stream, input, microphone, and clipboard regression.
- `OldTargetApi` removed from the baseline.

## Phase 3: Dependency Upgrades

Upgrade and commit each dependency family separately in this order:

1. JmDNS: discovery, duplicate hosts, interface changes, reconnect.
2. Gson: settings and stored-host backward-compatible deserialization.
3. Bouncy Castle: pairing, certificates, key persistence, and reconnect.
4. OkHttp 4 to 5: requests, cancellation, timeouts, connection reuse, and threading.
5. Glide 3 to 5: host artwork, cache, lifecycle, and failure placeholders.

An available update is not sufficient justification for a major upgrade. If behavior
cannot be preserved and verified, retain the current version and document the concrete
blocker instead of suppressing the warning broadly.

### Exit gate

The dependency's feature-specific tests and the full Release gate pass after each
upgrade. Remove only the matching `GradleDependency` entry.

## Phase 4: Orientation, Large Screens, and Windowing

- Make host, settings, help, and file-transfer screens adaptive.
- Preserve the user's stream orientation intent without relying on the platform to
  honor a fixed activity orientation.
- Verify phones, tablets, foldables, split-screen, freeform windows, and
  picture-in-picture.
- Treat temporary platform compatibility properties as isolated, documented bridges
  with a removal condition, not permanent architecture.

### Exit gate

The four orientation-related findings are resolved, and stream input coordinates remain
aligned in every tested window configuration.

## Phase 5: Rendering and Launcher Resources

- Profile menu and overlay rendering before modifying complex vector paths.
- Simplify redundant vector nodes and excessive path precision without visual drift.
- Convert only unsuitable large vectors to density-aware raster assets.
- Inspect `activity_game_display.xml` and `ax_floating_view.xml`; remove a background
  only when it is proven redundant.
- Provide adaptive launcher foreground/background resources and verify common vendor
  masks.
- Keep vector and `nodpi` resources where they are semantically correct instead of
  manufacturing duplicate density folders.

### Exit gate

Visual comparison passes on phone, tablet, TV, and the in-stream overlay. The 25
resource/rendering findings are fixed or precisely documented as intentional.

## Phase 6: Intentional API Attributes and Baseline Closure

- Review every `UnusedAttribute` against its platform API level and runtime purpose.
- Prefer version-qualified resources when they make behavior clearer.
- Otherwise use the narrowest supported annotation or Lint configuration with a
  reason; never suppress the whole file or issue globally.
- Audit all remaining baseline entries against current source locations.

### Exit gate

- Zero unexplained baseline findings.
- Zero new Lint findings.
- All retained exceptions have a narrow scope and an adjacent rationale.
- `verifyLocal` and `verifyConnected` pass.
- Both Release APKs are built, hashes recorded, install/upgrade behavior checked, and
  release logging confirmed not to emit per-frame input, audio, or clipboard traffic.

## Commit Policy

Tests and documentation, each SDK target step, each dependency family, windowing work,
rendering work, and final baseline cleanup are separate commits. Every commit must leave
the repository buildable; release-gate commits must also leave the complete verification
suite green.
