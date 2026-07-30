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
