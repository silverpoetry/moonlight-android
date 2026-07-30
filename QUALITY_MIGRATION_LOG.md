# Android Quality Migration Log

This log records the evidence for each independently committed migration step. It does
not replace the final release matrix in `QUALITY_IMPROVEMENT_PLAN.md`.

## Compile SDK 36

- Commit: `79dfa84e`
- `compileSdk`: 36
- `targetSdk`: 34
- Android Gradle Plugin: 8.10.1
- Gradle: 8.14.5
- `verifyLocal`: passed
- Lint: no new findings; the reviewed baseline remained at 67 entries

## Target SDK 35

- `targetSdk`: 35
- Android 15 edge-to-edge behavior is handled with explicit system-bar and display-cutout
  insets.
- The stream content root owns the safe rectangle, so the video surface, local cursor,
  overlays, and touch coordinates use the same geometry.
- Fullscreen stream behavior preserves the existing user opt-in for drawing into display
  cutouts. Visible system bars are protected in multi-window mode.
- Settings, help, host, app, file-share, about, and credits screens retain safe content
  areas under enforced edge-to-edge.
- The app does not request audio focus, so Android 15's target-gated audio-focus
  restriction does not require a compatibility path.

Verification on 2026-07-30:

- `verifyLocal`: passed
- JVM tests: passed, including four stream-inset policy tests
- All four root/non-root debug/release Lint variants: passed
- Both root and non-root Release builds: passed
- Lint: no new findings; the reviewed baseline remained at 67 entries
- Connected device: `192.168.3.125:5555`, Xiaomi `23116PN5BC`, API 36
- `connectedNonRootDebugAndroidTest`: 31 passed
- `connectedRootDebugAndroidTest`: 31 passed

The connected tests establish lifecycle and component compatibility. Real streaming,
input-coordinate alignment, clipboard, and microphone checks remain part of the final
manual release gate.

## Target SDK 36

- `targetSdk`: 36
- Removed the application-wide predictive-back opt-out.
- Custom back actions use one lifecycle-safe platform dispatcher adapter on Android 13+
  while retaining `onBackPressed()` only as the API 21-32 fallback.
- The stream activity and its menu dialog both dispatch to the existing
  `handleStreamBackPressed()` implementation, preserving the original two-second
  double-back exit behavior across both windows.
- File upload still blocks back navigation while an upload is active.
- Help-page history and settings language-apply behavior retain their custom back
  semantics.
- Android 16 large-screen orientation overrides do not alter stream geometry: the
  resizable layouts continue to derive video and input coordinates from the actual
  stream view bounds.
- API 36 does not expose a local-network runtime permission; existing `NsdManager`,
  HTTP, RTSP, and UDP networking therefore require no speculative permission path.

Verification on 2026-07-30:

- `verifyLocal`: passed
- All four root/non-root debug/release Lint variants: passed
- Both root and non-root Release builds: passed
- Lint: no new findings; `OldTargetApi` and three removed back-opt-out attribute
  entries were deleted from the baseline, leaving 63 reviewed entries
- Phone: `192.168.3.125:5555`, Xiaomi `23116PN5BC`, API 36
  - non-root instrumentation: 31 passed
  - root instrumentation: 31 passed
- Large-screen device: `192.168.3.3:42815`, Xiaomi `24018RPACC`, API 36,
  2032 x 3048 at 400 dpi
  - non-root instrumentation: 31 passed
  - root instrumentation: 31 passed

Both devices were locked during automation, so tests that require a visible activity
or active stream were not claimed as passed. Predictive-back gestures, double-back
stream exit, rotation/window transitions, and live input alignment remain in the final
manual release gate.

## Dependency: JmDNS 3.6.3

- Upgraded `org.jmdns:jmdns` from 3.5.9 to 3.6.3.
- The Android-specific network-topology delegate remains installed and the `_nvstream`
  service identity, port, and IPv4/IPv6 accessors retain the API used by discovery.
- Targeted JmDNS compatibility tests: passed.
- `verifyLocal`: passed.
- Lint: no new findings; the matching `GradleDependency` entry was removed, leaving
  62 reviewed baseline entries.
- Android 14+ uses the platform `NsdManager`; a live JmDNS discovery check still
  requires an API 21-33 device and remains a final compatibility-matrix item.

## Dependency: Gson 2.14.0

- Upgraded `com.google.code.gson:gson` from 2.10.1 to 2.14.0.
- Existing shortcut JSON keeps its field initializers when older records omit newer
  fields, ignores unknown future fields, and preserves every persisted shortcut field
  across a serialize/deserialize round trip.
- Gson is also used for update metadata and credits. Stored hosts, certificates, and
  pairing keys are not Gson-backed and are unaffected by this migration.
- Targeted shortcut JSON compatibility tests: passed.
- `verifyLocal`: passed.
- Lint: no new findings; the matching `GradleDependency` entry was removed, leaving
  61 reviewed baseline entries.

## Dependency: Bouncy Castle 1.84

- Upgraded `bcprov-jdk18on` and `bcpkix-jdk18on` together from 1.77 to the current
  upstream release, 1.84.
- Pairing certificate generation, PEM encoding, PKCS#8 key storage, RSA challenge
  signing, and the lightweight AES primitive retain the APIs and formats used by the
  app.
- Android packaging excludes only the duplicate Java 9 OSGi descriptor shared by the
  three BC runtime artifacts; Android does not consume that descriptor.
- The existing Lint exception for an unused BC compatibility trust manager is
  version-independent but remains limited to the `bcpkix-jdk18on` JAR. Application
  trust-manager implementations remain subject to the security check.
- JVM certificate and AES compatibility tests: passed.
- API 36 device (`192.168.3.125:5555`) isolated identity generation/reload test:
  passed for non-root and root variants.
- `verifyLocal`: passed.
- Lint: no new findings; the two matching `GradleDependency` entries were removed,
  leaving 59 reviewed baseline entries.

## Platform dependency prerequisite: AndroidX

- Replaced the legacy support annotations, `FileProvider`, `DocumentFile`, and
  AppCompat widgets with their AndroidX counterparts.
- Replaced the support test runner with AndroidX Test Runner 1.7.0 and JUnit
  Extensions 1.3.0.
- The release runtime dependency graph contains no `com.android.support` artifacts.
- The FileProvider metadata key remains `android.support.FILE_PROVIDER_PATHS` because
  this is the stable key intentionally retained by AndroidX. Samsung's
  `com.sec.android.support.multiwindow` metadata is vendor-defined and also remains
  unchanged.
- All newly enabled AppCompat vendor checks were fixed in source: compatibility
  drawable loading, `SwitchCompat`, compound drawables, and explicit About-screen
  click listeners. No new findings were added to the baseline.
- Jetifier is temporarily enabled only for Glide 3. Bouncy Castle is excluded from
  Jetifier because it is AndroidX-neutral and its Java 25 multi-release classes are
  newer than Jetifier's bytecode reader. Both temporary settings are scheduled for
  removal with the isolated Glide migration.
- `verifyLocal`: passed.
- API 36 phone (`192.168.3.125:5555`): 32 non-root and 32 root instrumentation tests
  passed.
- API 36 large-screen device (`192.168.3.3:42815`): 32 non-root and 32 root
  instrumentation tests passed.
- The AndroidX FileProvider and document-file transfer tests passed on both devices.
  The debug About activity launched without a runtime exception. Devices were locked,
  so visible styling and settings interaction remain part of the final manual gate.
- Lint: no new findings; the reviewed baseline remains at 59 entries.

## Dependency: OkHttp 5.4.0

- Upgraded `com.squareup.okhttp3:okhttp` from 4.12.0 to the current upstream release,
  5.4.0.
- Migrated every request-body construction site to the non-deprecated body-first API;
  request methods, media types, byte content, transfer headers, and empty-body
  semantics remain unchanged.
- The merged debug and release manifests contain OkHttp's
  `androidx.startup.InitializationProvider` entry, confirming that the Android runtime
  integration is packaged rather than merely compiling against the common artifact.
- Device loopback tests cover synchronous POST request framing, headers and response
  consumption, asynchronous callback response ownership, read timeout, and
  cancellation. Test clients explicitly release their dispatcher and connection-pool
  resources.
- `verifyLocal`: passed.
- API 36 phone (`192.168.3.125:5555`): 35 non-root and 35 root instrumentation tests
  passed.
- API 36 large-screen device (`192.168.3.3:42815`): 35 non-root and 35 root
  instrumentation tests passed.
- Lint: no new findings; the matching `GradleDependency` entry was removed, leaving
  58 reviewed baseline entries.
- Live host discovery, pairing, streaming, clipboard, and file-transfer requests remain
  part of the final manual release gate because the automated checks do not establish
  an active Sunshine session.

## Dependency: Glide 5.0.7

- Upgraded `com.github.bumptech.glide:glide` from the unmaintained 3.8.0 release to
  5.0.7 and migrated the custom-image listener to the current `Drawable`,
  `GlideException`, and `DataSource` API.
- Removed the Glide 3-only `GlideBitmapDrawable` branch. Glide 5 returns the platform
  `BitmapDrawable`, which was already the primary extraction path.
- Added the canonical generated `AppGlideModule` configuration and disabled obsolete
  manifest module discovery.
- Preserved Glide 3's opaque-image `RGB_565` preference so full-screen backgrounds and
  image grids do not silently double their bitmap memory use after the upgrade.
- Removed the temporary Jetifier configuration. The release runtime dependency graph
  contains no `com.android.support` artifacts.
- Device tests verify that the generated module is active, opaque JPEGs decode to
  `RGB_565` when hardware bitmaps are disabled, and drawable requests return the
  platform `BitmapDrawable`.
- `verifyLocal`: passed.
- API 36 phone (`192.168.3.125:5555`): 37 non-root and 37 root instrumentation tests
  passed.
- API 36 large-screen device (`192.168.3.3:42815`): 37 non-root and 37 root
  instrumentation tests passed.
- Lint: no new findings; the matching `GradleDependency` entry was removed, leaving
  57 reviewed baseline entries.
- Visible background, remote custom box art, and credits-avatar rendering remain part
  of the final unlocked-device manual release gate.

## Orientation, large screens, and windowing

- Replaced three scattered `Game` orientation requests with one
  policy/controller boundary. The obsolete alternate-rendering activity and its
  dedicated orientation branch were subsequently retired.
- Compact full-screen devices preserve the existing portrait, automatic-rotation,
  landscape, on-screen-controller, and native-resolution behavior.
- Windows with `smallestScreenWidthDp >= 600`, split/freeform windows, and
  picture-in-picture explicitly follow the user's full orientation preference. The
  policy does not depend on Android honoring a fixed request on Android 16 large
  screens.
- Squarish-window decisions now use the activity's current configuration dimensions
  instead of the physical display mode, so a resized or moved window is not classified
  using stale full-display geometry.
- Entering or leaving multi-window and every handled configuration change reapplies the
  centralized policy. Stream sizing, insets, video aspect ratio, touch mapping, and
  local cursor mapping continue to derive from the actual stream view bounds.
- The only fixed-orientation calls are confined to the documented compact full-screen
  controller method; its Lint suppression is narrow and does not cover activities or
  manifests globally.
- Pure JVM tests cover compact regular windows, compact squarish windows, portrait and
  landscape native streams, automatic rotation, on-screen controller precedence, SBS,
  invalid dimensions, and adaptive windows.
- `verifyLocal`: passed.
- API 36 phone (`192.168.3.125:5555`): 37 non-root and 37 root instrumentation tests
  passed.
- API 36 large-screen device (`192.168.3.3:42815`): 37 non-root and 37 root
  instrumentation tests passed.
- All three `SourceLockedOrientationActivity` entries and the `DiscouragedApi` manifest
  entry were removed, leaving 53 reviewed baseline entries.
- Both devices were locked. Visible rotation, split/freeform resizing, picture-in-picture,
  and live stream input-coordinate alignment were not claimed as passed and remain in
  the final manual release gate.

## Rendering resources and launcher assets

- Replaced the oversized controller vector paths with compact, equivalent even-odd
  geometry. The button and directional-pad cutouts remain transparent, and an
  instrumentation test verifies those semantics rather than relying on file size alone.
- Corrected the intrinsic size of the computer and settings action icons to `24dp`.
- Removed two redundant drawable backgrounds. The secondary-display presentation now
  owns one explicit black window fallback through `SecondaryDisplayTheme`; the stream
  view remains responsible for rendered content.
- Added density-specific legacy launcher icons, an Android 8+ adaptive icon, and an
  Android 13+ monochrome layer. The adaptive foreground stays inside the documented
  safe zone, which is checked by instrumentation.
- Added reproducible source artwork and
  `tools/generate_android_image_assets.ps1` for launcher, TV-banner, and placeholder
  raster variants. Generated density assets are committed so release builds do not
  depend on an image tool being present.
- Lint removed eight reviewed findings: two `Overdraw`, two `VectorRaster`, two
  controller `VectorPath`, `IconLauncherShape`, and `IconMissingDensityFolder`.
  The baseline now contains 45 entries pending the intentional-attribute closure.
- `verifyLocal` prerequisites passed: unit tests, non-root release Lint, Android-test
  compilation, and both debug APK assemblies.
- API 36 devices `192.168.3.125:5555`, `192.168.3.3:42815`, and
  `192.168.3.79:5555`: 39 non-root and 39 root instrumentation tests passed on every
  device. Root tests were invoked directly with AndroidJUnitRunner after Gradle's UTP
  wrapper stalled before starting instrumentation; this separates runner evidence
  from the wrapper failure.
- The devices were not used for unlocked visual inspection. Launcher shape, TV banner,
  streaming fallback, overlay appearance, and secondary-display behavior remain in the
  final manual release gate.

## Intentional attributes, baseline closure, and release gate

- Removed redundant per-activity predictive-back opt-ins. Target API 36 enables
  predictive back by default, while the supported callback registration remains in the
  shared navigation boundary.
- Consolidated Samsung DeX's explicit resizable declaration at application scope.
  Manifest-only optional capabilities are locally suppressed on exactly the
  `application` and `Game` elements with adjacent runtime rationale.
- Moved stream-surface focus behavior to `values-v26` and overlay
  `preferKeepClear` behavior to `values-v33`. API 36 instrumentation verifies the
  selected runtime attributes after inflating the real stream layout.
- Retained 16 small, static 12–24dp action vectors. Each resource has its own adjacent
  `VectorPath` rationale; the previously pathological controller path was simplified
  rather than suppressed.
- Lint reported zero errors or warnings while the old baseline was still present and
  identified all 45 records as obsolete. The baseline file and Gradle baseline
  configuration were then deleted. `warningsAsErrors` remains enabled, with no Lint
  baseline.
- Replaced every direct application call to `android.util.Log` with the centralized
  `DebugLog` facade. Only that facade imports the platform logger, and it is gated by
  `BuildConfig.DEBUG`. Release unit tests prove both tagged logging and `LimeLog` emit
  no records; this includes render, controller-input, audio-haptics, microphone, and
  clipboard paths.
- `verifyLocal`: passed with 148 JVM tests across all four build variants, zero
  failures, errors, or skips; all four Lint variants passed with no baseline; both
  unminified Release APKs built.
- `verifyConnected` with `ANDROID_SERIAL=192.168.3.125:5555`: passed 40 non-root and
  40 root instrumentation tests. Direct AndroidJUnitRunner verification also passed
  the same 40 tests for both variants on `192.168.3.3:42815` and
  `192.168.3.79:5555`.
- Release upgrade/install checks passed on all three devices. Non-root Release upgraded
  in place with unchanged `firstInstallTime`; Root Release installed cleanly. Every
  installed package reports version code 314, version name `12.1-260725`, and target
  API 36.

Final Release artifacts:

| Flavor | Size | SHA-256 |
| --- | ---: | --- |
| `nonRootRelease` | 15,966,413 bytes | `C4EA081E1654D94F41F9661A123CD3CB25BFA7F3BAF03415E3939F1FADDDD6A3` |
| `rootRelease` | 15,987,524 bytes | `91BE46C30E24A5223EE9F0BFEC72B1D66AF779170A7D4FF87EF365961560B5A3` |

All three devices reported `NotificationShade` as the focused window and a dreaming
lockscreen during the final gate. Visible launcher/vendor-mask comparison, TV launcher,
secondary-display presentation, split/freeform/PiP transitions, and a live Sunshine
stream covering input, microphone, and bidirectional clipboard remain manual release
checks. They are not represented as passed by the automated evidence above.

## Stream session lifecycle boundary

- Added a single-use `StreamSessionController` and explicit `SessionState` model for
  `CREATED`, `STARTING`, `STREAMING`, `TERMINATED`, `STOPPING`, `STOPPED`, and
  `FAILED`.
- Removed `Game`'s independent `attemptedConnection`, `connecting`, and `connected`
  flags. Surface readiness, picture-in-picture eligibility, input availability, and
  cleanup now query the same state machine.
- A connection that is stopped while the HTTP launch is running, while waiting for
  another common-c session, or while native startup is running can no longer publish a
  late `connectionStarted` callback back into the Activity.
- Replaced the process-global raw `Semaphore` with ownership-bearing, idempotent
  leases. Only the `NvConnection` that acquired the common-c lease may interrupt,
  stop, clean up, or release the process-global bridge.
- `NvConnection.stop()` now interrupts and joins its own pending startup worker before
  reporting cleanup complete. It never releases another connection's permit.
- Pure JVM tests cover single-use startup, exactly-once stop scheduling, late callback
  rejection, cleanup after termination/failure, exclusive lease ownership, and
  idempotent lease close.

Verification on 2026-07-30:

- Targeted session lifecycle and lease tests: passed.
- `verifyLocal`: passed with 180 JVM test executions across all four build variants,
  zero failures, errors, or skips.
- All four root/non-root debug/release Lint variants: passed with no findings.
- Both unminified root and non-root Release APKs: built successfully.
- Live cancellation during HTTP launch, native startup, and an active stream remains a
  manual device regression gate; the automated tests do not claim those host-dependent
  paths were exercised.
