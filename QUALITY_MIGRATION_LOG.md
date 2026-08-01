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

## Typed stream-video settings boundary

- Added one immutable `StreamVideoSettings` snapshot and atomic state owner for
  resolution, FPS, bitrate, codec preference, HDR policy, window/display policy,
  and FSR presentation.
- Both the in-stream menu and app-list display dialog now receive snapshots and
  emit typed update intents. They no longer read default preferences or mutate
  `PreferenceConfiguration`.
- The platform can recreate the display dialog without restoring
  process-local setter fields: it resolves a narrow lifecycle host from the
  attached Activity, and nested resolution/FPS/bitrate dialogs return results
  through a restorable Fragment target.
- Resolution, FPS, bitrate, portrait mode, external-display mode, and the three
  FSR values are persisted by the explicit Apply action as one ten-key
  transaction. Immediate radio options write only their owned key, and a stale
  dialog draft cannot roll those values back.
- Unknown future FSR strings remain byte-for-byte unchanged unless the user
  explicitly selects a replacement. Invalid enum/range data resolves to the
  canonical schema default without crashing settings or stream composition.
- Moved Moonlight's bitrate-default calculation into a pure policy using
  overflow-safe pixel arithmetic. Schema version 3 migrates the historical Mbps
  key to canonical Kbps without overwriting a newer value.
- Custom resolutions now cross a platform-independent repository port. The
  Android adapter copies mutable preference sets, filters invalid/unbounded
  dimensions, returns immutable snapshots, and confines the historical named
  store to the composition boundary.
- Added architecture rules that prevent the three display-settings fragments
  from reintroducing legacy preference, Android preference, adapter, or raw
  `SharedPreferences` dependencies.

Verification on 2026-07-31:

- Focused stream-video, migration, audio-intent, and architecture tests: passed.
- `verifyLocal --rerun-tasks`: passed; 1,036 JVM test executions across all four
  variants, zero failures, errors, or skips; all Lint variants passed; both
  unminified Release APKs built.
- API 34 emulator: 103 root and 103 non-root instrumentation tests passed,
  including real `SharedPreferences` migration and custom-resolution adapter
  coverage.
- Release artifacts:

| Flavor | Size | SHA-256 |
| --- | ---: | --- |
| `nonRootRelease` | 16,001,802 bytes | `10BE7EC1400D2117BD0D30E85F52A9A482FB5044B7DB31D969B91D8F0BFEE364` |
| `rootRelease` | 16,022,357 bytes | `304ADD71FC543D74F4BE0A2F7399C4BFB77BBA9628F11534BEA99DAC400BAA96` |

- Opening both dialog entry points and reconnecting a live Sunshine stream after
  Apply remain manual behavior checks; the automated evidence does not claim
  those host-dependent interactions passed.

## Typed stream-menu presentation boundary

- Removed `GameMenuHost.getStreamPreferences()`. The in-stream action catalog
  now consumes `StreamUiSettings`, and the virtual-gamepad and virtual-key
  buttons query the actual overlay owners for current visibility.
- Separated persisted launch policy from mutable session presentation.
  `ControllerSettings` and `VirtualControlSettings` determine initial overlay
  creation; menu toggles and picture-in-picture hiding no longer mutate
  `PreferenceConfiguration` as a surrogate runtime state store.
- Added the virtual-key startup default and built-in-shortcut catalog policy to
  their canonical typed schemas, loaders, immutable snapshots, and update
  intents.
- Replaced the orientation adapter's legacy aggregate dependency with
  `StreamOrientationRequest`. The composition root projects current gamepad
  visibility, the immutable active-stream geometry, portrait policy, and
  automatic-orientation policy into that request.
- Deleted the displaced `PreferenceConfiguration` fields and keys for
  on-screen-gamepad visibility, virtual-key startup visibility, built-in
  shortcut hiding, portrait policy, and automatic orientation.
- Added an architecture rule preventing the stream menu and orientation
  boundary from depending on preference packages, Android settings adapters,
  or raw `SharedPreferences`.

Verification on 2026-07-31:

- Focused typed UI/virtual-control, orientation-policy, and architecture tests:
  passed; both root and non-root production and instrumentation sources
  compiled.
- `verifyLocal --rerun-tasks`: passed with all 193 tasks executed. Each of the
  four root/non-root debug/release variants ran 263 JVM tests, for 1,052
  executions total with zero failures, errors, or skips. All Lint variants
  passed, and both unminified Release APKs built.
- `verifyConnected --rerun-tasks` on the API 34 emulator: all 296 tasks
  executed; 103 non-root and 103 root instrumentation tests passed with zero
  failures, errors, or skips.
- Release artifacts:

| Flavor | Size | SHA-256 |
| --- | ---: | --- |
| `nonRootRelease` | 16,003,006 bytes | `95C93DC319832B3EBA638688A7BB33897042DB5927E9CD5EE6377946132AE404` |
| `rootRelease` | 16,022,999 bytes | `41211E1EEBD32FB1759CB51EAD94FBB26D9D98A57ED88C930901032E1416ED30` |

- Opening a live stream, toggling both overlays, entering/leaving
  picture-in-picture, and exercising compact-window orientation remain the
  host-dependent manual regression gate. Automated evidence does not claim
  those interactions passed.

## Typed stream-menu card layout

- Replaced `GameMenuCardConfiguration`'s direct default-preference access with
  an immutable `GameMenuCardLayout`, a consumer-owned repository port, and a
  typed settings-backed implementation owned by the stream Activity.
- The layout stores only bounded stable card IDs. The pure configuration
  policy filters removed IDs, deduplicates order, preserves explicit hidden
  state, and applies catalog defaults to newly discovered actions/shortcuts.
  Labels, icons, callbacks, and shortcut payloads are never serialized into
  this document.
- The card editor receives an immutable initial state and emits one save
  result. It and the Fragment cannot address `SettingsRepository`,
  `SharedPreferences`, preference packages, or Android settings adapters.
- Extended the typed settings schema with defensive immutable string
  collections. The Android adapter copies both reads and writes so callers
  cannot alias `SharedPreferences`' mutable set instances.
- Schema version 4 converts the former action-only comma-separated order and
  hidden set to namespaced card references, preserves an existing current
  layout, deletes the v1 keys, handles late values introduced by downgrade,
  and commits the migration atomically.
- Replaced an initially detected API-24-only `java.util.Optional` dependency
  with `GameMenuCardLayoutLoadResult`; the production path remains compatible
  with the application's API 21 minimum without a Lint suppression.
- Architecture tests keep the card settings model, codec, IDs, repository
  port, and settings-backed implementation platform-independent and prevent
  the UI from reclaiming persistence ownership.

Verification on 2026-07-31:

- Focused schema, migration, codec, repository, catalog-resolution, and
  architecture tests passed. Corrupt JSON, wrong shape, duplicates, stale
  IDs, unknown cards, bounded collections, mutable aliasing, migration
  idempotence, and current-layout precedence are covered.
- `verifyLocal --rerun-tasks`: passed with all 193 tasks executed. Each of the
  four root/non-root debug/release variants ran 277 JVM tests, for 1,108
  executions total with zero failures, errors, or skips. All Lint variants
  passed, including the API 21 gate, and both unminified Release APKs built.
- `verifyConnected --rerun-tasks` on the API 34 emulator: all 296 tasks
  executed; 105 non-root and 105 root instrumentation tests passed with zero
  failures, errors, or skips. These include real `SharedPreferences`
  string-collection round-trip and schema-v4 upgrade coverage.
- Release artifacts:

| Flavor | Size | SHA-256 |
| --- | ---: | --- |
| `nonRootRelease` | 16,005,312 bytes | `AF442CDE4459FB55391C0DA2D1BBD8B2762AC286BEF72FB4449A9EB513A72B25` |
| `rootRelease` | 16,024,684 bytes | `098EA1D4CAEDC4211C683B6E67A4B433E3A7D060C45EB77A9C44FE5C44FF2C26` |

- Opening the live card editor, saving/reordering cards, and visually
  confirming an upgrade from a device that still contains the v1 layout
  remain manual UI checks. Automated evidence does not claim those
  interactions passed.

## Canonical stream-menu shortcut documents

- Replaced the stream-menu Fragment and shortcut catalog's two direct named
  `SharedPreferences` readers plus ad-hoc Gson/JSON parsing with immutable
  `GameMenuShortcut` values and a consumer-owned
  `GameMenuShortcutRepository`.
- Shortcut values defensively copy all key arrays and explicitly distinguish
  Moonlight protocol key codes from Android key codes. IDs, names,
  descriptions, chord lengths, key ranges, document size, document count,
  version, identity class, and editability are bounded and validated.
- The pure shortcut catalog now combines fixed built-ins with one supplied
  repository snapshot. The card catalog, shortcut picker, and mapper cannot
  address preference storage or the Android adapter.
- The Android adapter migrates the historical `specialPrefs/special_key`
  array first, then the dynamically keyed `quick_axi_keyAssemble` values in
  deterministic key order. It commits one canonical document before cleaning
  either source and retains imported/custom stable IDs, so existing card
  references survive.
- Once a canonical document exists, legacy values are cleanup-only and are
  never used as a fallback. Invalid documents fail closed; invalid individual
  entries are isolated and counted without logging their payloads. A malformed,
  wrong-typed, or unsupported-version canonical document cannot be overwritten
  by a subsequent save/delete intent.
- The mutable `GameMenuQuickBean` remains behind one explicit UI mapper because
  it is still shared by the separate virtual-keyboard layout editor. It is no
  longer a persistence or catalog-domain model for stream-menu shortcuts.
- Architecture tests enforce a platform-independent shortcut domain and keep
  UI/catalog code away from Android preference adapters and raw
  `SharedPreferences`.

Verification on 2026-07-31:

- Focused immutable-model, canonical-codec, legacy-codec, catalog, architecture,
  and real `SharedPreferences` migration tests passed. Coverage includes
  defensive copies, both key representations, malformed and duplicate entry
  isolation, unsupported versions and wrong primitive storage types,
  deterministic bounded two-source migration, idempotent reload, canonical
  save/delete, invalid-document preservation, and absence of a legacy
  fallback.
- `verifyLocal --rerun-tasks`: passed with all 193 tasks executed. Each of the
  four root/non-root debug/release variants ran 292 JVM tests, for 1,168
  executions total with zero failures, errors, or skips. All Lint variants
  passed, including the API 21 gate, and both unminified Release APKs built.
- `verifyConnected --rerun-tasks` on the API 34 emulator: all 296 tasks
  executed; 110 non-root and 110 root instrumentation tests passed with zero
  failures, errors, or skips. Both flavors exercised the canonical repository
  against real named Android preferences.
- Release artifacts:

| Flavor | Size | SHA-256 |
| --- | ---: | --- |
| `nonRootRelease` | 16,010,378 bytes | `E36986CA3C5B056C2BAFA1656BBC8AA30C65560194C0F2B86A5CAAD051257FD8` |
| `rootRelease` | 16,029,547 bytes | `287354661E641A061C58DD2DCEB694364F3C44E83F2D5FA23B0AA2432FB4DC4B` |

- Opening a live stream, migrating real historical shortcut data, adding and
  deleting a shortcut, executing both shortcut representations, and
  confirming that an existing customized first-page shortcut card remains
  selected are manual UI/host checks. Automated evidence does not claim those
  interactions passed.

## Remove dead virtual-control element library

- Proved that `GameListKeyBoardFragment` had no production, resource,
  Manifest, or test entry point and that its named
  `keyboard_axi_keyAssemble` store had no other reader. It was not the active
  virtual-control persistence path.
- Deleted the unreachable Fragment and its direct `SharedPreferences`/Gson
  CRUD path. Editable controls continue to persist only through the existing
  bounded, atomic `VirtualControlLayoutRepository` document.
- Moved new element identity into the platform-independent layout domain.
  Existing layout IDs remain unchanged; newly created keyboard, mouse, and
  gamepad elements retain the historical prefix but use UUID suffixes instead
  of collision-prone millisecond timestamps.
- Added a focused identity test. The existing architecture rule keeps the
  layout identity domain independent of Android.

Verification on 2026-07-31:

- Repository-wide reference search confirmed there is no remaining entry
  point or reader for the deleted Fragment/store.
- `verifyLocal --rerun-tasks`: passed with all 193 tasks executed. Each of the
  four root/non-root debug/release variants ran 293 JVM tests, for 1,172
  executions total with zero failures, errors, or skips. All Lint variants
  passed, and both unminified Release APKs built. The former unchecked-cast
  compiler warning from the deleted Fragment is absent.
- `verifyConnected --rerun-tasks` on the API 34 emulator: all 296 tasks
  executed; 110 non-root and 110 root instrumentation tests passed with zero
  failures, errors, or skips.
- Release artifacts:

| Flavor | Size | SHA-256 |
| --- | ---: | --- |
| `nonRootRelease` | 16,009,442 bytes | `6BF65491311C1C142EE956E7E7F51AF11FFB9744DB8E00994AFED5B3A7F8DE0A` |
| `rootRelease` | 16,028,292 bytes | `35DEB6B546C661B00849DACAFBBFC2987AAC13EA30D8A89959AAF99041BA41CB` |

- Creating, saving, reloading, and operating every virtual keyboard/gamepad
  element type in a live stream remains the manual layout regression gate.
  Automated evidence does not claim that host-dependent interaction passed.

## Typed stream-session composition

- Removed `Game`'s public mutable `PreferenceConfiguration` field and deleted
  the temporary `LegacyPreferenceSettingsAdapter`. The Activity composition
  root now bootstraps one `SettingsRepository` and composes immutable video,
  display, decoder, input, controller, audio, UI, transfer, and virtual-control
  snapshots.
- Completed the existing domain models instead of introducing another
  cross-domain bag. Typed keys now own local-system-cursor and adaptive input
  throttling policy; stretch/cutout/native geometry and SOPS launch policy;
  full-range and refresh-rate-reduction policy; and picture-in-picture,
  connection-warning, and latency-toast policy.
- Added direct display and decoder loaders. Resolution repair and native-mode
  classification remain in the resolution codec; audio channel layout and UI
  overlay policy are projected into the decoder contract without Android or
  legacy-preference dependencies.
- Replaced mutation of the stored frame-pacing value with
  `StreamFramePacingPolicy`, which returns a separate effective session mode
  and target FPS for the active display. The decoder continues to receive the
  requested mode, preserving established renderer behavior, while subsequent
  display policy observes the effective fallback exactly where the old
  session-only mutation took effect.
- Preserved platform behavior explicitly: the Android settings bootstrap runs
  idempotent schema migration and the Android 12 fresh-install motion-sensor
  safety default; one Android HDR compatibility adapter is shared by stream
  composition and the legacy settings screen for the known broken Shield
  firmware.
- Removed obsolete compatibility writes when toggling absolute mouse mode,
  direct-touch sensitivity, and on-screen-control rumble. Live state owners
  and typed update intents are now the only runtime mutation path.
- Added an executable architecture rule preventing `Game` from depending on
  `PreferenceConfiguration`.

Verification on 2026-07-31:

- Focused loader, default/normalization, native-resolution, frame-pacing,
  Android compatibility, and architecture tests passed.
- `verifyLocal --rerun-tasks`: all 193 tasks executed successfully. Each of
  the four root/non-root debug/release variants ran 304 JVM tests, for 1,216
  executions total with zero failures, errors, or skips. All Lint variants
  passed, including the API 21 gate, and both unminified Release APKs built.
- `verifyConnected --rerun-tasks` on the API 34 emulator: all 296 tasks
  executed successfully; 110 non-root and 110 root instrumentation tests
  passed with zero failures, errors, or skips.
- Release artifacts:

| Flavor | Size | SHA-256 |
| --- | ---: | --- |
| `nonRootRelease` | 16,010,803 bytes | `FCB5718E23D81FD882CCF25017C9E27194CA786D45915EABF5B3A42EBBB304F9` |
| `rootRelease` | 16,030,099 bytes | `3A92BA14F1BFE4BCE5BB4DF1D7F7801BB4EB94F8851DB18609FF1F516E15BF46` |

- Opening a live HDR/non-HDR stream, exercising capped-FPS display selection,
  entering picture-in-picture, checking local/native cursor modes, and
  observing warning/latency-toast behavior remain manual host/device checks.
  Automated evidence does not claim those interactions passed.

## Delete the legacy application-wide settings bag

- Deleted `PreferenceConfiguration` after proving that stream composition,
  input, controller, audio, transfer, decoder, and virtual-control runtime
  consumers had already moved to immutable domain snapshots. No production
  source can depend on the removed type; an architecture rule enforces that
  repository-wide boundary.
- Added immutable `AppPresentationSettings` for language, theme, app-icon
  density, optional host/app-list background and blur, background file, and
  host-list label. Canonical keys and validation live in the pure settings
  domain. Android adapters alone compute the TV/phone icon default and migrate
  the legacy language value into Android's per-app locale API.
- Removed the settings dependency from `PcGridAdapter`; narrowed
  `AppGridAdapter` to the one boolean it consumes. Consolidated duplicated
  Glide/background/blur code in `ScreenBackgroundPresenter`.
- Moved accessibility key diagnostics into `InputSettings`. The accessibility
  service caches the typed value and observes only its canonical key, removing
  full preference parsing from every key-down callback.
- Moved Android GameManager suppression into `StreamUiSettings` and passes the
  immutable session decision into platform calls. HDR high-brightness policy
  likewise comes from the existing stream-video snapshot instead of a storage
  reread on each HDR callback.
- Replaced the legacy class's remaining static utility surface with focused
  policies: `StreamDisplayGeometry`, `StreamSettingsResetter`,
  `AndroidStreamDefaults`, `AndroidHdrCompatibility`, canonical typed keys,
  and the existing resolution/bitrate codecs. Decoder-crash recovery preserves
  the historical reset scope and has a pure contract test.
- Removed three instrumentation suites whose only purpose was verifying that
  already-tested typed snapshots could be copied back into the deleted
  property bag.

Verification on 2026-07-31:

- Repository-wide production and test source search found no consumer of
  `PreferenceConfiguration` beyond the architecture rule naming the forbidden
  type.
- `verifyLocal --rerun-tasks`: all 193 tasks executed successfully. Each of
  the four root/non-root debug/release variants ran 310 JVM tests, for 1,240
  executions total with zero failures, errors, or skips. All Lint variants
  passed, including the API 21 gate, and both unminified Release APKs built.
- `verifyConnected --rerun-tasks` on the API 34 emulator: all 296 tasks
  executed successfully; 105 non-root and 105 root instrumentation tests
  passed with zero failures, errors, or skips.
- Release artifacts:

| Flavor | Size | SHA-256 |
| --- | ---: | --- |
| `nonRootRelease` | 16,009,834 bytes | `A38940C776C73810EBAF1C36A007F74501ED26D29CAD869BA1C0E8BF87762B87` |
| `rootRelease` | 16,028,340 bytes | `4239B3CD491676EDDC764019F19699BEE68175D38305119C488CC54F33F1C83B` |

- Opening the host/app grids on phone and TV, selecting a legacy non-system
  language, loading and blurring a custom background, changing icon density,
  toggling accessibility diagnostics, and observing GameManager/HDR behavior
  on supporting hardware remain manual UI/platform checks. Automated evidence
  does not claim those interactions passed.

## Close the stream-coordinate geometry audit

- Inventoried every production path that submits mouse, touch, pen, or native
  touchpad coordinates and the complete locally rendered host-cursor path in
  `docs/architecture/STREAM_COORDINATE_AUDIT.md`.
- Confirmed the common-parent invariant: touchscreen/input Views, the stream
  View, and the cursor overlay are direct siblings under the inset-adjusted
  content root. All cross-sibling points use `ViewCoordinateMapper`; basis
  vectors use its translation-free path.
- Confirmed that `NvConnection` remains the sole owner of absolute cursor
  position. It clamps once under its ordering lock, then gives the identical
  reference position to the local overlay listener and common-c.
- Documented why native touchpad contacts stay normalized to the physical
  gesture surface and must not use video viewport mapping.
- Added an architecture rule restricting the sibling mapper to the approved
  input and stream-UI packages. Removed a duplicate cursor-recovery comment;
  no coordinate algorithm or protocol behavior changed.

Verification on 2026-07-31:

- The new architecture rule passed after production source compilation.
- The immediately preceding complete `verifyLocal` and `verifyConnected`
  gates covered the audited implementation: 1,240 JVM executions, 105
  non-root and 105 root instrumentation tests, all Lint variants, and both
  Release APKs passed. The audit-only delta changed comments, documentation,
  and the architecture test; it did not change runtime bytecode behavior.
- Physical phone/tablet checks across rotation, zoom, cutouts, native
  touchpad gestures, stylus contacts, and host cursor shapes remain the manual
  hardware matrix. Automated evidence does not claim those checks passed.

## Remove the unused upscaling pipeline and type the settings screen

- Removed the optional post-processing renderer vertically: its shaders,
  GLES implementation, intermediate Surface, lifecycle flags, settings keys,
  settings and in-stream menu controls, performance-overlay fields, and tests.
  Stream video now has one render path: decoder output directly to the system
  Surface.
- Reduced stream display and video settings to policy that is still supported.
  Display Apply now owns seven persisted values instead of carrying three
  inactive rendering values.
- Bound persisted settings-screen rows to the canonical `SettingKey` catalog.
  XML describes presentation; setting type, default, validation, and storage
  ownership remain in the settings domain.
- Replaced the AppCompat-only switch used under the framework Material theme
  with the matching framework control. Ordinary setting changes update the
  existing row in place; collection-changing settings preserve scroll
  position when the screen must be rebuilt.

Verification on 2026-07-31:

- Production, JVM-test, and instrumentation-test source contains no remaining
  reference to the removed upscaling feature, and no matching source or asset
  path remains.
- `verifyLocal --rerun-tasks`: all 193 tasks executed successfully. Each of
  the four root/non-root debug/release variants ran 308 JVM tests, for 1,232
  executions total with zero failures, errors, or skips. All Lint variants
  passed, including the API 21 gate, and both unminified Release APKs built.
- `verifyConnected --rerun-tasks` on the API 34 emulator: all 296 tasks
  executed successfully; 107 non-root and 107 root instrumentation tests
  passed with zero failures, errors, or skips.
- Instrumentation covers settings activity launch, visible switch geometry,
  and scroll-position stability across a switch update.
- Release artifacts:

| Flavor | Size | SHA-256 |
| --- | ---: | --- |
| `nonRootRelease` | 15,933,885 bytes | `C0F87F732DF3644306B55F8443B2950216A6676A136AFC94B7F516F038377EDF` |
| `rootRelease` | 15,954,332 bytes | `CC5D83A31D1CD8822D16393D2C2239BDBAE358F29E5F56B5382AAC3A9BA19D9B` |

## Begin runtime composition and render ownership

- Moved physical display-mode selection out of `Game` into the pure
  `StreamDisplayModeSelector`. The selector preserves the established
  candidate order and compatibility rules while making refresh-rate and
  resolution decisions deterministic under JVM tests.
- Added the Activity-scoped `StreamRenderSurfaceController` as the single owner
  of Surface callback registration, readiness, frame-rate hints, decoder stop
  preparation, and callback detachment. `Game` no longer implements
  `SurfaceHolder.Callback` or owns parallel Surface lifecycle flags.
- The extracted render path remains synchronous on the Android main thread and
  adds no queue, worker, or allocation to decoded-frame delivery.

Verification on 2026-07-31:

- `verifyLocal --rerun-tasks`: all 193 tasks executed successfully. Each of
  the four root/non-root debug/release variants ran 318 JVM tests, for 1,272
  executions total with zero failures, errors, or skips. All Lint variants
  passed and both Release APKs built.
- The API 34 emulator passed 107 non-root and 107 root instrumentation tests.
  A complete `verifyConnected` invocation then passed after an installation
  transport timeout in the first root attempt was retried.
- Replaced the controller handler's duplicated sixteen-slot reservation loops
  and two mutable masks with `ControllerSlotAllocator`. Initial enumeration,
  first-input ownership transfer, exhaustion fallback, release/reuse,
  single-controller mode, and on-screen-controller participation now have one
  state owner and JVM fixtures.
- Re-ran `verifyLocal --rerun-tasks` after the slot extraction. Each of the
  four variants ran 324 JVM tests, for 1,296 executions with zero failures,
  errors, or skips. A clean `verifyConnected --rerun-tasks --no-daemon` run
  then passed all 107 non-root and 107 root instrumentation tests.
- Extracted Android battery-state conversion and unchanged-sample suppression
  into the immutable `ControllerBatteryReport`, leaving platform battery
  acquisition and transport emission in the controller adapter.
- Extracted controller key-layout policy into `ControllerButtonMapper`.
  Device-specific mappings, Joy-Con opt-in corrections, raw D-pad fallback,
  soft-keyboard handling, face-button swapping, and stateful Start/Select
  fallback behavior now have focused JVM fixtures and add no allocation to the
  input-event path.
- Re-ran `verifyLocal --rerun-tasks` after the battery and mapping extraction.
  All 193 tasks passed; each root/non-root debug/release variant ran 342 JVM
  tests, for 1,368 executions with zero failures, errors, or skips. A clean
  `verifyConnected --rerun-tasks --no-daemon` run passed all 107 non-root and
  107 root instrumentation tests.
- Extracted HEVC/AV1 acceptance and color-default decisions into
  `DecoderSelectionPolicy`. Removed an unused metered-network constructor
  parameter and AV1 performance fallback branches that were unreachable while
  AV1 remained explicit-opt-in, without changing codec selection behavior.
- Re-ran `verifyLocal --rerun-tasks`: all 193 tasks passed, with 347 JVM tests
  in each root/non-root debug/release variant (1,388 executions total). A clean
  `verifyConnected --rerun-tasks --no-daemon` run passed all 107 non-root and
  107 root instrumentation tests.
- Added immutable `DecoderCapabilityProfile` ownership for direct submit,
  per-codec reference-frame invalidation, prior-crash fallback, and optimal
  slice aggregation. The renderer no longer keeps parallel mutable capability
  fields after construction.
- `verifyLocal --rerun-tasks` passed all 193 tasks after the capability
  extraction. Each root/non-root debug/release variant ran 350 JVM tests
  (1,400 executions total) with zero failures, errors, or skips.
- Added `DecoderStatisticsTracker` as the single owner of active, previous,
  and cumulative video-stat windows. Per-frame renderer/input updates remain
  lock-free and allocation-free; deterministic timestamps now drive FPS and
  window tests.
- `verifyLocal --rerun-tasks` passed all 193 tasks with 354 JVM tests in each
  root/non-root debug/release variant (1,416 executions total). A clean
  `verifyConnected --rerun-tasks --no-daemon` run passed all 107 non-root and
  107 root instrumentation tests.
- Added `CodecRecoveryCoordinator` as the owner of recovery severity,
  promotion, retry budget, and input/render/Choreographer quiescence state.
  MediaCodec calls and the existing recovery monitor remain in the renderer;
  no new worker, wait, or hot-path allocation was introduced.
- `verifyLocal --rerun-tasks` passed all 193 tasks with 360 JVM tests in each
  root/non-root debug/release variant (1,440 executions total). A clean
  `verifyConnected --rerun-tasks --no-daemon` run passed all 107 non-root and
  107 root instrumentation tests.
- Extracted the controller mouse-mode transition table into the stateful
  `ControllerMouseEmulationTranslator`. A/B mouse buttons, D-pad and desktop
  keys retain their press/release semantics; stick-click chords remain
  press-only. Each controller context owns one translator, while the transport
  output adapter and chord arrays are reused without per-report allocation.
- Replaced the inherited split-controller analog aggregation with
  `ControllerAnalogInputCombiner`. The upstream code selected the
  highest-magnitude candidate and then bitwise-ORed it into the accumulated
  value, which could manufacture an axis value that no device reported.
  Trigger bytes were also compared as signed Java values despite being
  unsigned protocol samples. The new pure policy assigns the selected axis
  directly and compares triggers as unsigned values, with boundary fixtures
  covering `0xff` and `Short.MIN_VALUE`.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks. Each
  root/non-root debug/release variant ran 366 JVM tests, for 1,464 executions
  total with zero failures, errors, or skips; all Lint variants and both
  unminified Release APKs passed.
- `verifyConnected --rerun-tasks --no-daemon` completed successfully on the
  API 34 emulator. The generated result suites record 107 non-root and 107
  root instrumentation tests with zero failures, errors, or skips.
- Consolidated scheduled stick, high-resolution scroll, and trigger-repeat
  output into each context's existing `ControllerMouseEmulationTranslator`.
  The established cubic response curve, sensitivity scaling, Y-axis
  directions, stick-selection policy, and per-report trigger repeat are locked
  by numeric fixtures. A reusable work vector replaces the previous
  `Vector2d` allocation on every 50 ms report.
- The follow-up `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks
  with 369 JVM tests in each variant (1,476 executions total), all Lint
  variants, and both unminified Release APKs. The matching
  `verifyConnected --rerun-tasks --no-daemon` run passed all 296 tasks and
  both 107-test root/non-root instrumentation suites.
- Added `ControllerMouseEmulationSession` as the single owner of the 50 ms
  report lifecycle. Activation cancels stale callbacks before scheduling,
  deactivation and destruction reject late callbacks, and an Android
  `Handler` adapter is the only platform dependency.
- Fixed input-device context migration to restore both the active state and
  its scheduled reporter. The previous code copied only a boolean before
  destroying the old callback, leaving controller mouse mode visibly enabled
  but no longer producing stick or trigger reports after a device-change
  callback.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 373 JVM
  tests in each variant (1,492 executions total), all Lint variants, and both
  Release APKs. A final incremental `verifyLocal` rebuilt the Release outputs
  after the last null-contract cleanup and also passed.
- The corresponding full `verifyConnected --rerun-tasks --no-daemon` run
  passed all 296 tasks; the generated API 34 suites record 107 non-root and
  107 root tests with zero failures, errors, or skips.
- Extracted `ControllerArrivalReport` as the immutable owner of controller
  protocol type, supported-button flags, capability flags, and the
  clickpad-emulation requirement. `InputDeviceContext` now samples Android
  hardware facts and emits the finished report instead of interleaving
  platform calls with protocol mutation.
- Fixtures cover complete PlayStation capabilities, non-PlayStation motion
  type fallback, pre-Android-12 legacy rumble behavior, and touchpads without
  a physical clickpad. Existing Android 12 battery assumptions, pre-Android
  14 PlayStation RGB handling, Shield extension capabilities, and all
  controller button additions are preserved.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 377 JVM
  tests per root/non-root debug/release variant (1,508 executions total), all
  Lint variants, and both Release APKs.
- `verifyConnected --rerun-tasks --no-daemon` passed all 296 tasks on the API
  34 emulator; the non-root and root suites each ran 107 instrumentation tests
  with zero failures, errors, or skips.
- Extracted `ControllerHapticsPolicy` from platform feedback code. Controller
  audio-haptics eligibility, ordinary-rumble suppression, the keep-rumble
  override, and Kishi-only selective suppression now form one tested decision
  table.
- Extracted `ControllerRumbleAmplitudes` for protocol-to-Android amplitude
  conversion. Dual/quad motor ordering, trigger channels, the established
  80%/33% single-motor mix, zero detection, and unsigned fallback-strength
  scaling are pure fixtures; Android feedback methods retain only capability
  checks and effect submission.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 386 JVM
  tests per variant (1,544 executions total), all Lint variants, and both
  Release APKs. `verifyConnected --rerun-tasks --no-daemon` then passed all
  296 tasks and both 107-test root/non-root API 34 suites.
- Added `ControllerMotionSession` as the sole owner of requested motion report
  rates and sensor registration lifecycle. It bounds invalid sampling rates,
  retains requests while sensors temporarily disappear, restores them after the
  established one-second device-settle interval, rejects stale delayed work by
  generation, emits a neutral gyroscope report on suspension, and migrates
  state without creating a second registration path.
- Physical input-device sensors and the optional on-device virtual-controller
  sensors now use the same session. The default context participates in focus
  suspension and handler shutdown, registration handles remember the exact
  `SensorManager` that owns them, and failed Android registrations are no longer
  retained as if they were active.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 392 JVM
  tests per variant (1,568 executions total), all Lint variants, and both
  unminified Release APKs. `verifyConnected --rerun-tasks --no-daemon` then
  passed all 296 tasks and both 107-test root/non-root API 34 suites.
- Extracted `ControllerMotionSampleTransformer` from the Android sensor
  callback. Raw-sample duplicate suppression, every display rotation, device
  coordinate correction, and gyro radians-to-degrees conversion are now pure,
  deterministic, and allocation-free after listener construction. The first
  legitimate all-zero sample is no longer dropped merely because Java arrays
  initialize to zero.
- Extracted the customized gyro-to-right-stick curve into one stateful
  `ControllerGyroStickTranslator` per controller context. Its established
  deadzone, 1.5-power response, horizontal gain, low-pass coefficient,
  inversion, and protocol clamp have numeric fixtures. Releasing the required
  trigger resets both output and filter state.
- Replaced the handler-wide force-gyro left-trigger sample and smoothing state
  with one atomic trigger value per protocol slot and one translator per
  controller context. Concurrent or split multi-controller reports can no
  longer gate or bias another controller's gyro output.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 402 JVM
  tests per variant (1,608 executions total), every Lint variant, and both
  unminified Release APKs. `verifyConnected --rerun-tasks --no-daemon` then
  passed all 296 tasks and both 107-test root/non-root API 34 suites.
- Added immutable `ControllerAxisProfile` ownership for controller stick,
  trigger, and hat assignment. The Android adapter now reports only complete
  axis-pair capabilities and maps the selected semantic axes back to Android;
  trigger precedence and the old/Linux DualShock distinction no longer mutate
  a partially initialized device context across nested branches.
- Fixtures preserve dedicated-trigger precedence, brake/gas and
  brake/throttle controllers, RX/RY versus Z/RZ fallback, centered trigger
  semantics, both DualShock layouts, unnamed-device behavior, and the rule that
  a hat is usable only when both axes exist.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 409 JVM
  tests per variant (1,636 executions total), all Lint variants, and both
  unminified Release APKs. `verifyConnected --rerun-tasks --no-daemon` then
  passed all 296 tasks and both 107-test root/non-root API 34 suites.
- Extracted `ControllerDeviceQuirks` from input-device attachment. ADT-1 and
  ASUS back/mode mappings, legacy trigger deadzones, early NVIDIA search keys,
  Razer Serval buttons, old Xbox Bluetooth firmware detection, and
  Thrustmaster home-key handling now resolve from immutable sampled facts.
- `ControllerHandler` probes only the ASUS Start/Menu keys and Xbox gas-axis
  fact required by that policy, then applies one finished profile to the
  runtime context and `ControllerButtonMapper`. Tests lock both corrected and
  ordinary-device behavior so branch ordering cannot silently change it.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 417 JVM
  tests per variant (1,668 executions total), all Lint variants, and both
  unminified Release APKs. `verifyConnected --rerun-tasks --no-daemon` then
  passed all 296 tasks and both 107-test root/non-root API 34 suites.
- Added `StreamInputLifecycleController` as the single owner of the Activity's
  input resume, pause, finishing stop, routing detachment, controller destroy,
  and keyboard-listener unregistration sequence. Repeated lifecycle callbacks
  are idempotent and a finishing or destroyed session cannot restart routing.
- Teardown remains intentionally two-stage: touch/motion callbacks detach
  before session and media destruction, while controller resources stay alive
  until the audio renderer has been released; only then are controller devices
  destroyed and the keyboard listener lease returned. JVM traces cover normal
  cycles, finishing, partial startup, staged ordering, and repeated destroy.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 421 JVM
  tests per variant (1,684 executions total), all Lint variants, and both
  unminified Release APKs. `verifyConnected --rerun-tasks --no-daemon` then
  passed all 296 tasks and both 107-test root/non-root API 34 suites.
- Added `UsbDriverSessionController` as the sole owner of the stream Activity's
  USB service binding and callback endpoint. A binding accepted before
  `onServiceConnected()` is now still unbound during teardown, endpoint
  callbacks are revoked before `ControllerHandler` destruction, reconnect and
  replacement are explicit, and all terminal paths are idempotent.
- Replaced the USB Binder's independently mutable configure/listener/start API
  with one session attach/detach contract. `UsbDriverCallbackRegistry` publishes
  the input and state listeners as one atomic immutable snapshot and assigns a
  generation-scoped lease, preventing a stale overlapping Activity from
  clearing a newer session's callbacks while also providing cross-thread
  visibility to USB report threads.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 430 JVM
  tests per variant (1,720 executions total), all Lint variants, and both
  unminified Release APKs. `verifyConnected --rerun-tasks --no-daemon` then
  passed all 296 tasks and both 107-test root/non-root API 34 suites with zero
  failures, errors, or skips.
- Extracted `AndroidDecoderDiscovery` from `MediaCodecDecoderRenderer`. Decoder
  enumeration, AVC fallback, non-allow-listed HEVC/AV1 handling, AVC-versus-HEVC
  performance comparison, direct-submit/RFI facts, and slice preferences now
  produce one immutable selection before any codec lifecycle work begins. The
  renderer retains only the selected codecs and frozen capability profile.
- Added pure `DecoderPerformanceEvaluator` fixtures for Android's ordered
  capability evidence. A covering Q performance point accepts, a present but
  non-covering set rejects without optimistic fallback, absent point data falls
  through to M achievable FPS, unsupported dimensions reject, and only missing
  achievable-rate data reaches the size/rate capability API.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 437 JVM
  tests per variant (1,748 executions total), all Lint variants, and both
  unminified Release APKs. `verifyConnected --rerun-tasks --no-daemon` then
  passed all 296 tasks and both 107-test root/non-root API 34 suites with zero
  failures, errors, or skips.
- Extracted `ControllerVibrationRenderer` as the Android adapter for vibration
  target selection and effect submission. Android 12 dual/quad managers,
  Shield extensions, per-input legacy vibrators, internal-controller handset
  fallback, explicit device vibration, trigger channels, cancellation, and
  capability reporting now share one target owner instead of six mutable
  fields in every `InputDeviceContext`.
- Added immutable `SingleVibratorRumblePlan` fixtures for zero cancellation,
  the optional handset stop pulse, forced-strong priority over amplitude
  control, the existing two-motor mix, and the bounded 20 ms legacy PWM cycle.
  `ControllerHandler` now decides only which controller/player should receive
  feedback and delegates all Android rendering details.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 442 JVM
  tests per variant (1,768 executions total), all Lint variants, and both
  unminified Release APKs. `verifyConnected --rerun-tasks --no-daemon` then
  passed all 296 tasks and both 107-test root/non-root API 34 suites with zero
  failures, errors, or skips.
- Added `RazerKishiHapticsController` as the synchronized owner of optional USB
  audio-haptics sidecars. `ControllerHandler` no longer enumerates devices,
  opens connections, throttles refreshes, stores sidecars, or submits their
  frames. Backend, clock, policy, and diagnostic adapters keep Android I/O at
  the edge while tests drive the lifecycle deterministically.
- The first on-demand refresh is now guaranteed instead of implicitly relying
  on system uptime already exceeding 1.5 seconds. An existing but stopped
  sidecar is explicitly closed before replacement, failed starts are closed,
  stale/disabled devices are removed, and refresh/submission/destruction cannot
  concurrently mutate the active-device map.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 449 JVM
  tests per variant (1,796 executions total), all Lint variants, and both
  unminified Release APKs. `verifyConnected --rerun-tasks --no-daemon` then
  passed all 296 tasks and both 107-test root/non-root API 34 suites with zero
  failures, errors, or skips.
- Added `ControllerBatterySession` as the per-device owner of controller
  battery polling. Its generation-scoped scheduled ticks make enable/disable,
  controller recreation, in-flight callbacks, and terminal destruction
  deterministic; a callback canceled by a settings change can no longer
  requeue itself indefinitely.
- Live controller-settings updates now start or stop reporting on every active
  physical controller immediately. Teardown always cancels the battery
  session regardless of the latest preference value, closing the previous
  enabled-then-disabled lifecycle leak.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 455 JVM
  tests per variant (1,820 executions total), all Lint variants, and both
  unminified Release APKs. `verifyConnected --rerun-tasks --no-daemon` then
  passed all 296 tasks and both 107-test root/non-root API 34 suites with zero
  failures, errors, or skips.
- Extracted controller RGB-light ownership into `ControllerLedSession` and
  `AndroidControllerLedTarget`. The pure session preserves protocol byte
  conversion and the latest desired color; the platform target caches the RGB
  light set, opens one Android 12+ session lazily, and closes it idempotently.
- Input-device recreation no longer moves a `LightsSession` created by the old
  device object into the replacement context. It snapshots the desired color,
  closes the old hardware lease, binds a new target, and reapplies the color;
  no target write occurs when the host has not supplied a color.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 461 JVM
  tests per variant (1,844 executions total), all Lint variants, and both
  unminified Release APKs. `verifyConnected --rerun-tasks --no-daemon` then
  passed all 296 tasks and both 107-test root/non-root API 34 suites with zero
  failures, errors, or skips.
- Split controller battery sampling and emission from `ControllerHandler`.
  `AndroidControllerBatterySource` reads one Android S `BatteryState` or maps
  SHIELD extension facts; `ControllerBatteryReporter` converts the sample,
  suppresses duplicates including unavailable capacity, and emits through a
  narrow protocol sink.
- Added pure fixtures for SHIELD wired, wireless, both, full, charging,
  discharging, not-charging, unknown, and unavailable-capacity behavior, plus
  reporter fixtures for absent, duplicate, changed, and invalid samples. The
  two-minute scheduler and protocol-visible state conversion remain unchanged.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 471 JVM
  tests per variant (1,884 executions total), all Lint variants, and both
  unminified Release APKs. `verifyConnected --rerun-tasks --no-daemon` then
  passed all 296 tasks and both 107-test root/non-root API 34 suites with zero
  failures, errors, or skips.
- Added generic `ControllerMotionRegistrations` ownership for the active
  accelerometer and gyroscope leases. Each replacement removes the previous
  listener from the manager that actually registered it before consulting the
  current manager; missing sensors and rejected registrations leave no stored
  listener, while unsupported motion types cannot disturb an active lease.
- `AndroidControllerMotionBackend` now contains sensor lookup, listener
  creation, sampling-period registration, unregistration, on-device
  orientation classification, and neutral-gyroscope delivery. The realtime
  callback and protocol ordering are unchanged and no queue or thread was
  added.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 478 JVM
  tests per variant (1,912 executions total), all Lint variants, and both
  unminified Release APKs. `verifyConnected --rerun-tasks --no-daemon` then
  passed all 296 tasks and both 107-test root/non-root API 34 suites with zero
  failures, errors, or skips.
- Extracted `ControllerMotionEventProcessor` as the allocation-free owner of
  the realtime motion branch. Duplicate suppression still precedes settings
  access; device sensors retain four-way coordinate correction; controller
  sensors read display rotation only for force-gyro stick mapping; native
  motion and force-gyro outputs remain mutually exclusive.
- Fixtures lock the left-trigger threshold at 200, translator reset and zero
  stick emission below that threshold, accelerometer suppression while force
  gyro is active, raw gyro axis use, output values, and conditional rotation
  access. Android's `SensorEventListener` now forwards only its three values to
  the processor and introduces no allocation, lock, queue, or thread.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 484 JVM
  tests per variant (1,936 executions total), all Lint variants, and both
  unminified Release APKs. `verifyConnected --rerun-tasks --no-daemon` then
  passed all 296 tasks and both 107-test root/non-root API 34 suites with zero
  failures, errors, or skips.
- Replaced the three independently mutable controller-number fields in every
  context with `ControllerSlotLease`. Fixed/shared selection, allocator-backed
  reservation, assignment completion, release, fallback to player one, and
  device-context migration now pass through one state owner with explicit
  ordering and range invariants.
- Migration transfers reservation ownership to the replacement without
  releasing the slot or changing the host mask. The destroyed context retains
  its number and assigned routing snapshot for a callback already in flight,
  but no longer owns the allocator reservation, preventing both accidental
  reassignment and double release.
- Split-device assignment now selects exactly once: a matching joystick shares
  its established number, while player one is selected only after both adjacent
  association candidates fail. This preserves the DS4 touchpad fallback without
  relying on an overwrite of partially initialized slot state.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 491 JVM
  tests per variant (1,964 executions total), all Lint variants, and both
  unminified Release APKs. `verifyConnected --rerun-tasks --no-daemon` then
  passed all 296 tasks and both 107-test root/non-root API 34 suites with zero
  failures, errors, or skips.
- Extracted controller slot-selection decisions into pure
  `ControllerAssignmentPolicy` fixtures. Built-in inputs remain fixed to player
  one, external joysticks reserve only in multi-controller mode, auxiliary
  inputs always attempt split-device association, and USB controllers preserve
  the existing single/multi-controller behavior.
- Extracted the DS4-style split-device identity rule into
  `ControllerAssociationPolicy`. Tests reject absent and non-joystick
  candidates, equal-name duplicate controllers, and mismatched descriptors,
  while accepting only a differently named joystick with the same descriptor.
  Android orchestration preserves the forward-then-reverse adjacent-device
  search and creates or assigns the matched joystick context before sharing its
  established slot.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 500 JVM
  tests per variant (2,000 executions total), all Lint variants, and both
  unminified Release APKs. `verifyConnected --rerun-tasks --no-daemon` then
  passed all 296 tasks and both 107-test root/non-root API 34 suites with zero
  failures, errors, or skips.
- Moved Android controller-arrival capability sampling out of the mutable
  device context into `AndroidControllerArrivalProbe`. Platform key support,
  joystick/gamepad hat ranges, touchpad and clickpad inference, SDK-gated
  vibration and RGB support, runtime sensor state, and SHIELD recognition now
  produce one immutable facts snapshot for the existing pure
  `ControllerArrivalReport` policy.
- Added `ControllerTypeResolver` with fixtures proving that Microsoft, Sony,
  and Nintendo identities bypass native guessing, while unknown identities
  delegate the original vendor and product IDs unchanged. Arrival transport
  emission and subsequent battery-session startup remain in their original
  order. Expected hidden-API unavailability now falls back silently instead of
  printing a release stack trace.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 502 JVM
  tests per variant (2,008 executions total), all Lint variants, and both
  unminified Release APKs. `verifyConnected --rerun-tasks --no-daemon` then
  passed all 296 tasks and both 107-test root/non-root API 34 suites with zero
  failures, errors, or skips.
- Extracted controller sensor-manager eligibility into
  `ControllerMotionSensorPolicy` and platform access into
  `AndroidControllerMotionSource`. Disabled settings and pre-Android-12 builds
  never touch the input-device sensor API; Android 12 probes only Sony and
  Nintendo devices; the original Android 12L no-probe behavior is explicit;
  Android 13+ accepts any vendor but binds only a manager that exposes an
  accelerometer or gyroscope.
- This preserves the defensive workaround for Android 12's
  `InputDeviceSensorManager` callback failure without leaving SDK/vendor policy
  embedded in context construction. No callback, scheduler, thread, or
  steady-state input work was added.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 507 JVM
  tests per variant (2,028 executions total), all Lint variants, and both
  unminified Release APKs. `verifyConnected --rerun-tasks --no-daemon` then
  passed all 296 tasks and both 107-test root/non-root API 34 suites with zero
  failures, errors, or skips.
- Extracted external/input-device classification into
  `ControllerExternalDevicePolicy` and `AndroidInputDeviceClassifier`. Fixtures
  preserve the Tinker Board external override; the SHIELD Portable, Archos,
  Xperia Play, NVIDIA v01.01/v01.02, and Logitech G Cloud internal mappings;
  the original case sensitivity; and platform fallback for ordinary devices.
- Android Q+ uses the public platform value, older releases isolate hidden-API
  reflection in the adapter, and an unavailable legacy result keeps the
  original safe external default without printing exception stacks in release
  logs. Context construction samples its classification once and reuses it for
  Back-button policy instead of querying the same device twice.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 511 JVM
  tests per variant (2,044 executions total), all Lint variants, and both
  unminified Release APKs. `verifyConnected --rerun-tasks --no-daemon` then
  passed all 296 tasks and both 107-test root/non-root API 34 suites with zero
  failures, errors, or skips.
- Added `AndroidControllerAxisProbe` as the only adapter from Android
  joystick/gamepad motion ranges to `ControllerAxisProfile`. It preserves the
  `SOURCE_JOYSTICK` then `SOURCE_GAMEPAD` fallback, every axis-pair probe, Sony
  button-C detection, and the complete profile-to-Android constant mapping.
- Initial enumeration, controller classification, context construction,
  trigger deadzone sampling, and controller-arrival Hat reporting now reuse the
  same adapter instead of carrying parallel range logic. The probe runs only on
  device attachment or change; no steady-state motion allocation, lock, queue,
  or thread was introduced.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 512 JVM
  tests per variant (2,048 executions total), all Lint variants, and both
  unminified Release APKs. `verifyConnected --rerun-tasks --no-daemon` then
  passed all 296 tasks and both 107-test root/non-root API 34 suites with zero
  failures, errors, or skips.
- Added `AndroidControllerDeviceProfileProbe` as the one-shot owner of physical
  controller identity, key support, touchpad ranges, axis profile, deadzones,
  device quirks, and immutable button-mapping policy. `InputDeviceContext`
  construction now binds this immutable profile to vibration, LED, battery,
  and motion lifecycle resources instead of depending on a long sequence of
  partially initialized mutable fields.
- Extracted trigger-deadzone normalization into
  `ControllerTriggerDeadzonePolicy`, preserving raw absolute driver values
  when correction is disabled and the established 13–30 percent accepted
  range when enabled. Boundary fixtures lock both paths.
- Made `ControllerButtonMapper` genuinely immutable. The ADT-1/ASUS fallback
  flags that learn from real Start and Select events now live in a dedicated
  `ControllerButtonMappingState` owned by each controller session, so no
  mutable runtime state leaks into the reusable device profile.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 516 JVM
  tests per variant (2,064 executions total), all Lint variants, and both
  unminified Release APKs. `verifyConnected --rerun-tasks --no-daemon` then
  passed all 296 tasks and both 107-test root/non-root API 34 suites with zero
  failures, errors, or skips.
- Extracted Android Back-button ownership into pure
  `ControllerBackButtonPolicy` and the platform-only
  `AndroidControllerBackButtonProbe`. Device identity and capability facts now
  produce an explicit handle/ignore/inspect-inventory result, so internal
  device enumeration occurs only for the branch that needs it.
- Policy fixtures preserve the Razer Serval exception, case-insensitive remote
  detection only without joystick axes, external axis/button requirements,
  and the internal no-gamepad, gamepad-with-Select, and gamepad-without-Select
  navigation outcomes. `AndroidControllerInputCapabilities` centralizes the
  shared gamepad source-bit interpretation used by discovery and this probe.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 521 JVM
  tests per variant (2,084 executions total), all Lint variants, and both
  unminified Release APKs. `verifyConnected --rerun-tasks --no-daemon` then
  passed all 296 tasks and both 107-test root/non-root API 34 suites with zero
  failures, errors, or skips.
- Added `ControllerChordEmulationState` as the allocation-free per-controller
  owner of legacy Select, Mode, and clickpad chords, the 100 ms bumper-release
  grace window, physical Mode/Select discovery, and deferred quit-combo
  completion. `InputDeviceContext` no longer exposes the six related flags and
  timestamps as independently mutable fields.
- Sequence fixtures lock exact-mask quit detection, release-only completion,
  Start+LB Select emulation, the inclusive left-bumper grace boundary,
  Select+LB clickpad emulation, both Mode fallback variants, synthesized-button
  release, and disabling fallbacks after physical buttons are observed. The
  realtime path still allocates nothing and introduces no scheduler or queue.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 528 JVM
  tests per variant (2,112 executions total), all Lint variants, and both
  unminified Release APKs. `verifyConnected --rerun-tasks --no-daemon` then
  passed all 296 tasks and both 107-test root/non-root API 34 suites with zero
  failures, errors, or skips.
- Replaced the duplicated physical-controller button-down and button-up
  switches with immutable `ControllerDigitalButtonMapping`. System, face,
  cardinal/diagonal D-pad, bumper, stick, digital-trigger, share, touchpad, and
  four paddle inputs now resolve through one symmetric protocol target table.
- The handler applies the returned enum singleton directly to the existing
  shared context, preserving USB/virtual/physical aggregation without adding a
  second mutable packet state. Hat-axis duplicates and digital triggers after
  analog-axis use still return before chord processing and packet emission;
  the realtime event path allocates nothing.
- Fixtures cover every mapping family, exact protocol masks, unadvertised and
  unknown paddle scan codes, unsupported keys, and horizontal, vertical, and
  diagonal Hat suppression rules.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 535 JVM
  tests per variant (2,140 executions total), all Lint variants, and both
  unminified Release APKs. `verifyConnected --rerun-tasks --no-daemon` then
  passed all 296 tasks and both 107-test root/non-root API 34 suites with zero
  failures, errors, or skips.
- Added `ControllerMouseModeActivationState` as the per-controller owner of
  mouse-mode/game-menu button timing. It records the initial Play down only,
  ignores repeats, applies the strict greater-than-750-ms boundary, verifies
  the configured activation target and its current protocol bit, and preserves
  immediate Mode-button activation.
- The Android handler now resolves the digital target once per event, asks the
  state for an activation decision before release mutation, and retains only
  the menu/toggle side effect. The established Select path's use of the latest
  Play timestamp remains explicit and fixture-locked rather than changing
  behavior during an ownership refactor.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 542 JVM
  tests per variant (2,168 executions total), all Lint variants, and both
  unminified Release APKs. `verifyConnected --rerun-tasks --no-daemon` then
  passed all 296 tasks and both 107-test root/non-root API 34 suites with zero
  failures, errors, or skips.
- Added `ControllerInputState` as the only mutable owner of each controller
  input source's protocol report. Digital buttons, chord results, physical and
  USB sticks, analog and digital triggers, Hat directions, touchpad click,
  virtual-controller replacement reports, gyro-stick output, packet
  aggregation, and mouse emulation now cross that boundary instead of
  mutating seven independent fields on `ControllerHandler` contexts.
- Moved negative-idle trigger activation history, deadzone conversion,
  analog-over-digital trigger precedence, and persistent Hat-axis discovery
  into the platform-independent state owner. Fixtures preserve the initial
  zero sample rule, unsigned trigger values, directional-bit isolation,
  independent button releases, and complete snapshot replacement. The
  realtime path still adds no allocation, lock, queue, thread, or reordered
  transport call.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 547 JVM
  tests per variant (2,188 executions total), all Lint variants, and both
  unminified Release APKs. `verifyConnected --rerun-tasks --no-daemon` then
  passed all 296 tasks and both 107-test root/non-root API 34 suites with zero
  failures, errors, or skips.
- Extracted `AndroidControllerInventory` as the one platform adapter for
  startup InputDevice/USB enumeration and controller-device routing. Stream
  configuration and runtime slot allocation now use the same adapter instead
  of a public static scan embedded in `ControllerHandler`; USB devices already
  exposed through Android remain de-duplicated.
- Added pure `ControllerInventoryMask` and
  `ControllerDeviceClassificationPolicy` fixtures. Initial reservations are
  consecutive and bounded to all sixteen protocol slots, OSC reserves player
  one, Android 11's source-less virtual device mirrors only a genuinely
  attached gamepad, and the established non-alphabetic-keyboard fallback is
  preserved. Removed the `hasGameController` field because it had no reader
  and therefore could not affect routing.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 556 JVM
  tests per variant (2,224 executions total), all Lint variants, and both
  unminified Release APKs. `verifyConnected --rerun-tasks --no-daemon` then
  passed all 296 tasks and both 107-test root/non-root API 34 suites with zero
  failures, errors, or skips.
- Completed the state contract for Android input-device recreation. Before an
  old context releases its platform resources, its complete protocol report,
  analog-trigger and Hat ownership, learned Start/Select mapping, active chord
  and quit sequence, bumper grace timestamps, mouse-mode hold clock,
  mouse-emulation previous output map, and gyro smoothing filter are restored
  into the replacement context.
- Mapping migration is monotonic: runtime discovery may turn a legacy fallback
  off, and a new hardware probe may independently reject it, but neither path
  can turn a rejected fallback back on. Mouse emulation keeps its emitted-state
  history so a held desktop mouse/key action is not repeated after recreation
  and its later release is still delivered. The existing slot transfer and
  host-visible no-unplug contract are preserved.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 565 JVM
  tests per variant (2,260 executions total), all Lint variants, and both
  unminified Release APKs. `verifyConnected --rerun-tasks --no-daemon` then
  passed all 296 tasks and both 107-test root/non-root API 34 suites with zero
  failures, errors, or skips.
- Extracted `AndroidControllerTouchpadAdapter` behind a narrow context target
  and protocol output. The handler now selects only an already recognized
  controller and supplies the typed live setting; Android source handling,
  clickpad packet mutation, per-contact dispatch, multi-contact MOVE,
  cancel-all, normalized coordinates and host capability results belong to the
  adapter.
- `ControllerTouchpadEventPolicy` fixtures preserve down/up action-index
  behavior, `FLAG_CANCELED` as one-contact cancellation, `ACTION_CANCEL` as
  cancel-all, API-gated primary button handling, and rejection of secondary or
  unknown actions. `ControllerTouchpadNormalizer` clamps valid hardware ranges
  and turns non-finite or non-positive ranges into neutral zero instead of
  allowing NaN or infinity to reach common-c. No input event is queued,
  sampled, or allocated into a collection.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 573 JVM
  tests per variant (2,292 executions total), all Lint variants, and both
  unminified Release APKs. `verifyConnected --rerun-tasks --no-daemon` then
  passed all 296 tasks and both 107-test root/non-root API 34 suites with zero
  failures, errors, or skips.
- Moved controller stick deadzone evaluation and protocol quantization into
  each source's `ControllerInputState`. Physical Android axes and USB driver
  reports now use the same radial, inclusive deadzone rule, preserve the
  historical host-owned non-renormalized response, share exact signed-short
  scaling, and invert Y once at the state boundary.
- Removed `ControllerHandler`'s shared mutable `Vector2d` scratch object. It was
  reachable from both main-thread Android input and USB driver callbacks, so
  concurrent updates could overwrite the other source's intermediate values.
  Per-context primitive conversion eliminates that race while retaining zero
  per-event allocation and adding no synchronization or scheduler hop.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 575 JVM
  tests per variant (2,300 executions total), all Lint variants, and both
  unminified Release APKs. `verifyConnected --rerun-tasks --no-daemon` then
  passed all 296 tasks and both 107-test root/non-root API 34 suites with zero
  failures, errors, or skips.
- Replaced the inherited physical-controller button-up `Thread.sleep()` with
  `ControllerButtonReleaseSession`. The minimum host-visible press interval is
  now measured from the actual down-packet submission rather than an Android
  event timestamp, and a quick release is delivered by the existing main
  looper without blocking input, rendering, settings, or lifecycle callbacks.
- The session uses fixed per-target primitive slots and one reusable drain
  callback. Fixtures lock the inclusive 25 ms boundary, repeat and duplicate-
  up behavior, multiple deadline ordering, same-target release-before-repress,
  ownership cancellation, device-context migration, and destruction. Normal
  presses remain immediate, and no steady-state collection allocation, lock,
  unbounded queue, or new thread was introduced.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 584 JVM
  tests per variant (2,336 executions total), all Lint variants, and both
  unminified Release APKs. `verifyConnected --rerun-tasks --no-daemon` then
  passed all 296 tasks and both 107-test root/non-root API 34 suites with zero
  failures, errors, or skips.
- Extracted `ControllerInputReportAggregator` from the duplicated Android/USB/
  virtual-source loops in `ControllerHandler`. It performs one ordered,
  allocation-free traversal for an assigned protocol slot, preserves the
  established mouse-mode partition and cross-mode virtual-default behavior,
  then emits exactly one report through a context-bound output.
- Aggregation intermediates remain method-local rather than becoming a shared
  scratch report, so overlapping input execution domains cannot corrupt each
  other's reduction. Fixtures lock assignment and slot filtering, mouse-mode
  ownership, the virtual-default exception, button union, unsigned trigger
  magnitude, signed axis magnitude, and neutral-report emission.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 587 JVM
  tests per variant (2,348 executions total), all Lint variants, and both
  unminified Release APKs.
- Extracted `UsbControllerInputAdapter` as the synchronous boundary between
  USB driver floats and controller protocol state. Stick, trigger, and touch
  inputs now reject non-finite values and clamp their documented ranges before
  deadzone evaluation or quantization; motion vector units remain untouched.
- Motion and touch both require an assigned controller number through the same
  target contract before transport emission. This closes the implicit-order
  hole where a driver's first gyroscope report could previously be sent with
  the unassigned `-1` slot because no ordinary state report had arrived yet.
  Fixtures verify state conversion and one-send behavior, bounded invalid
  input, assignment-before-motion, and assignment-before-touch ordering.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 591 JVM
  tests per variant (2,364 executions total), all Lint variants, and both
  unminified Release APKs.
- Added generic `UsbControllerLifecycleController` as the single owner of USB
  context publication, identity validation, replacement, removal, preparation
  rollback, and shutdown. A late remove from an old device instance can no
  longer delete a new device that reused the same integer controller ID, and a
  duplicate add from the same object is now idempotent instead of overwriting
  its live context.
- Store routing is revoked before advanced-haptics stop, slot release, or
  context destruction, so synchronous tail callbacks cannot enter a partially
  destroyed context. Normal removal/replacement still emits the established
  host unplug report; terminal stream shutdown destroys resources without
  reopening transport work. Seven fixtures cover all lifecycle transitions,
  exact side-effect ordering, rollback, late callbacks, and idempotence.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 598 JVM
  tests per variant (2,392 executions total), all Lint variants, and both
  unminified Release APKs.
- `verifyConnected --rerun-tasks --no-daemon` then passed all 296 tasks and
  both 107-test root/non-root API 34 suites with zero failures, errors, or
  skips, covering the aggregate input, USB adapter, and USB lifecycle batch.
- Added stateless `ControllerFeedbackRouter` for ordinary rumble, trigger
  rumble, and LED fan-out across all physical targets assigned to a protocol
  slot. Android and USB contexts are narrow delivery adapters; the handler no
  longer duplicates two platform loops and result aggregation for each
  feedback family.
- Rumble routing now models `NO_MATCH`, `MATCHED_UNAVAILABLE`, and `HANDLED`.
  A deliberate suppression because controller audio haptics owns the device is
  handled, not a missing vibrator, so it cannot accidentally activate handset
  fallback. True unavailable hardware and on-screen-controller-only feedback
  retain their distinct existing fallback paths. Six fixtures lock fan-out,
  slot filtering, aggregate result precedence, suppression, trigger motors,
  and LED delivery.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 604 JVM
  tests per variant (2,416 executions total), all Lint variants, and both
  unminified Release APKs.
- Extended the feedback target contract to standard audio-haptics motors,
  advanced USB audio frames, and advanced-mode enable/disable transitions.
  `ControllerHandler` now applies only the stopped/feature gates; one router
  traversal fans out to Android vibration or USB driver adapters and aggregates
  delivery without copying or retaining the audio frame.
- Targets with an active advanced stream do not also receive the standard
  motor approximation, preserving the existing ownership split. Unsupported
  Android and USB targets are explicit no-ops, while the separately managed
  Kishi sidecar remains outside the physical-controller registry. Three new
  fixtures lock standard delivery aggregation, advanced-frame fan-out, and
  enablement broadcast.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 607 JVM
  tests per variant (2,428 executions total), all Lint variants, and both
  unminified Release APKs.
- Moved adaptive-trigger capability and report ownership out of
  `ControllerHandler`. The handler now reads one immutable
  `ControllerSettings` snapshot and broadcasts typed mode/strength/frequency/
  start/end values; unsupported targets are explicit no-ops and the DualSense
  driver alone encodes and submits the hardware report.
- Added `DualSenseAdaptiveTriggerCommand` with a complete 48-byte report
  contract, symmetric L2/R2 effects, defensive byte clamping, and exact mode
  layouts for resistance, section trigger, automatic fire, and disabled or
  unknown modes. Removed the handler's `instanceof DualSenseController`, the
  generic public driver `sendCommand(byte[])` escape hatch, and unused sample
  effect builders. Six fixtures lock routing and byte-level encoding.
- One full Gradle invocation ended with its single-use daemon log truncated
  during execution and no failed task, Java crash file, or Gradle exception.
  An immediate complete incremental gate passed, followed by a fresh
  `verifyLocal --rerun-tasks --no-daemon` execution that passed all 193 tasks
  with 613 JVM tests per variant (2,452 executions total), all Lint variants,
  and both unminified Release APKs.

## Centralize stream launch and reconnect ownership

- Replaced mutable Activity-to-Activity launch assembly with one immutable
  `StreamLaunchRequest`, a pure admission/use-case boundary, and one Android
  Intent adapter. Every serialized extra is owned by
  `AndroidStreamLaunchContract`; only the target adapter depends on `Game`.
- `PcView` and `AppView` now own lifecycle-scoped launchers. Duplicate taps are
  rejected until a complete pause/resume round trip, a failed Android launch
  reopens admission, and a recent session is persisted only after Android
  accepts the destination Activity.
- Automatic reconnect now crosses Activities through one application-owned,
  compare-and-clear handoff. Stale consumers cannot erase a replacement, host
  mismatch/unavailability and invalid app identity are distinct outcomes, and
  a missing app ID can resolve only from the host's currently running app.
- Shortcut and TV entry points use the same stable extra contract, request
  validation, and certificate encoding behavior. Certificate serialization
  failure is terminal instead of silently launching without the pinned
  credential. The mixed-purpose `ServerHelper` and static
  `AutoReconnectHelper` production paths were deleted.
- `verifyLocal --rerun-tasks --no-daemon` passed all 193 tasks with 892 JVM
  tests per variant (3,568 executions total), every Lint variant, and both
  unminified Release APKs. A fresh connected run passed all 140 NonRoot and
  140 Root API 34 instrumentation tests with zero failures, errors, or skips.
  The NonRoot Release APK installed over the existing emulator application and
  cold-launched `PcView` in 409 ms with no fatal exception or ANR.

## Make the host repository boundary immutable

- Added a platform-independent `HostRepository` port and immutable
  `PersistedHost` value. Repository APIs now accept `HostId`/`HostRecord` and
  return immutable records; the remaining mutable NvHTTP `ComputerDetails`
  conversion is centralized in `LegacyHostDetailsAdapter`. The host service
  depends on the port rather than the SQLite implementation, while credentials
  remain structurally separate from observation metadata.
- Upgraded `computers4.db` from schema v5 to v6 with persistent user aliases.
  The migration preserves exact legacy UUID casing for metadata/credential
  joins while domain lookup remains case-insensitive. Portable snapshots retain
  aliases, read-only pre-v6 snapshots work without mutation, newer live and
  imported schemas plus corrupt sources are preserved, and restore validates
  the complete source before an atomic destination transaction.
- `verifyLocal --rerun-tasks --max-workers=1 --no-daemon` passed all 193 tasks
  with 896 JVM tests per variant (3,584 executions total), all four Lint
  variants, and both unminified Release APKs. Focused API 34 instrumentation
  passed all 15 repository migration/backup tests, then `verifyConnected`
  passed the complete 144-test NonRoot and 144-test Root suites with zero
  failures, errors, or skips.

## Make host runtime state immutable

- Replaced every service-owned mutable `ComputerDetails` field with an
  immutable `HostRuntimeSnapshot`. Per-host slots now publish a complete
  replacement after server-info, app-list, credential, reachability, and
  invalidation transitions instead of mutating shared protocol data.
- Added a credential-free `HostRuntimeObservation` and deterministic
  `HostRuntimeMergePolicy`. Probe data cannot replace a pinned certificate,
  partial observations retain repository-owned aliases and unobserved
  endpoints, reported external-port corrections create new endpoint values,
  and cross-host merges are rejected.
- Centralized remaining NvHTTP conversion in `LegacyHostRuntimeAdapter` and
  added an executable architecture rule preventing the host service or its
  nested lifecycle owners from storing mutable `ComputerDetails` fields.
- Replaced the Binder's mutable host parameters and return values with typed
  `HostId`, `HostEndpoint`, and `HostRuntimeSnapshot` contracts. Polling
  listeners now receive the same immutable value the service publishes, while
  manual admission no longer constructs a protocol DTO in the Activity.
- Migrated the host grid to retain and render immutable snapshots. UI actions
  create detached legacy values only for existing transport, shortcut,
  wake-on-LAN, and dialog adapters; no mutable protocol record is shared with
  or retained from the service callback.
- Added architecture gates that reject future mutable `ComputerDetails`
  parameters or return values on the Binder and listener contracts.
- The complete NonRoot Debug JVM suite passed all 907 tests with zero failures,
  errors, or skips. NonRoot Release Lint and assembly also passed; the signed
  Release APK installed over the existing application on the physical Xiaomi
  device and cold-launched `PcView` in 143 ms with the host service bound and
  no application fatal exception or ANR.
- The final clean Phase 7 gate passed `verifyLocal --rerun-tasks` across all
  193 tasks: 907 tests in each of NonRoot Debug, NonRoot Release, Root Debug,
  and Root Release (3,628 executions), all four Lint variants, and both
  unminified Release APKs. API 34 `verifyConnected` then passed all 146 NonRoot
  and 146 Root instrumentation tests with zero failures, errors, or skips.
