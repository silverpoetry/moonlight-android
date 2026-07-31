# Large-Scale Refactoring Roadmap

This document is the normative execution plan for separating this application
from its Axixi-derived architecture. It complements `ARCHITECTURE.md`: that
document defines the target rules, while this document defines the migration
order, evidence, and completion gates.

The objective is not to make classes smaller in isolation. The objective is to
create a system with explicit ownership, one-way dependencies, deterministic
realtime behavior, independently testable domains, and release evidence strong
enough to make large changes without relying on manual intuition.

## Non-negotiable migration rules

1. **Characterize before changing.** Protocol-visible or user-visible behavior
   receives a contract, state-machine, golden-trace, or regression test before
   its implementation is moved.
2. **One mutable state, one owner.** A state transition is accepted through one
   public boundary. Views, Activities, protocol callbacks, and repositories do
   not share writable state.
3. **One production path.** A migration slice delegates to the new owner and
   removes the displaced implementation in the same phase. Permanent dual
   implementations and hidden feature fallbacks are prohibited.
4. **Dependencies point inward.** UI and Android adapters depend on immutable
   models and consumer-owned contracts. Domain code does not depend on an
   Activity, Fragment, View, dialog, concrete transport, or preference API.
5. **Realtime paths remain explicit.** Input, audio, video, and transport
   callbacks follow `REALTIME_THREADING.md`; generic background abstractions
   must not obscure scheduling, ordering, backpressure, or allocation behavior.
6. **Structure and behavior are separate commits.** A move/rename does not also
   change an algorithm. A behavior change includes evidence that distinguishes
   the intended new result from the previous result.
7. **Delete compatibility debt on schedule.** This fork does not retain unused
   Axixi, GameSbs, or obsolete private-protocol compatibility. A temporary
   adapter has an owning phase and cannot survive that phase's completion.
8. **Package boundaries precede Gradle boundaries.** A dependency cycle is
   corrected in source before it can become a physical module relationship.
9. **No architecture by framework.** Java and Android Views remain during the
   core refactor. DI, Kotlin, Compose, reactive frameworks, and a database
   replacement require a separate ADR and demonstrated benefit.
10. **A green build is necessary, not sufficient.** Phase completion requires
    behavioral, lifecycle, thread, protocol, performance, upgrade, and hardware
    evidence appropriate to the changed domain.

## Target dependency graph

```text
app composition root
    -> feature UI adapters
        -> use-case controllers / immutable UI state
            -> domain contracts and models
                <- Android platform adapters
                <- Moonlight protocol adapters
                    -> common-c / JNI
```

The intended stabilized source domains are:

```text
ui -> stream, input, render, settings, hosts, transfer
stream -> protocol, render contracts
input -> protocol, viewport contracts, settings models
render -> protocol models, viewport
hosts -> protocol, settings/storage contracts
transfer -> protocol, storage contracts
platform adapters -> Android APIs
protocol adapters -> nvstream/common-c
```

Cross-domain calls use narrow ports or immutable values. A general-purpose
service locator, mutable static registry, event bus, or Activity callback web is
not an acceptable substitute for a dependency boundary.

## Delivery unit

Every migration slice uses the same sequence:

1. Record the current externally observable behavior.
2. Define the consumer-owned contract and immutable input/output models.
3. Implement the new owner behind that contract.
4. Wire it at the composition root.
5. Compare state transitions, protocol output, UI output, and performance.
6. Remove the old owner, bridge, and obsolete tests.
7. Add or strengthen an architecture rule that prevents regression.
8. Run the slice gate and commit one reviewable concern.

A slice is incomplete if callers can still bypass the new boundary.

## Phase 1 — Architecture and behavior baseline

**Status:** complete.

### Deliverables

- Normative architecture, realtime-threading, input-behavior, and migration
  documents.
- Accepted ADRs for package-first modularization and preserving Java/Views.
- Architecture dependency tests that only become stricter.
- Characterization tests for customized input behavior, especially gesture
  arbitration, force press, local cursor prediction, and external devices.
- A single local verification command covering both product flavors.

### Exit evidence

- Modified Axixi behavior is named and executable as tests.
- New production dependencies cannot silently point from a domain into UI.
- The verification gate is reproducible without IDE state.

## Phase 2 — Input orchestration and protocol ports

**Status:** complete.

### Deliverables

- A single routing owner for touchscreen, external pointer, keyboard, stylus,
  and controller precedence.
- Consumer-owned mouse, keyboard, touch, touchpad, and controller protocol
  ports instead of direct `NvConnection` access in gesture implementations.
- Explicit state machines for multi-finger keyboard deferral, touchpad
  promotion, long press, force press, cancellation, and focus loss.
- Golden protocol traces and disabled-feature no-latency tests.
- Allocation-conscious hot paths with no file, preference, or logging I/O.

### Exit evidence

- Every Android input entry point has one documented owner.
- Enabling a gesture delays only the ambiguous prefix it must arbitrate.
- Cancellation releases every remotely owned button/contact.
- The old `Game` input implementation is not a second production path.

## Phase 3 — Stream session and resource ownership

**Status:** complete.

### Deliverables

- Explicit single-use stream-session state machine.
- Generation-safe callback routing by execution domain.
- Idempotent stop/destroy behavior, including failed and partial startup.
- Dedicated ownership for diagnostics, Wi-Fi locks, UI effects, launch
  reporting, decoder/audio references, Surfaces, and delayed tasks.
- Activity teardown ordering that detaches callbacks before releasing resources.

### Exit evidence

- Late callbacks cannot mutate a destroyed or replacement session.
- Transport cleanup cannot run on or block the Android main thread.
- Each created media/platform resource has one matching owner and release path.
- Repeated stop/destroy and partial failures are deterministic.

## Phase 4 — Window, viewport, render, and coordinate geometry

**Status:** complete.

### Deliverables

- Named coordinate spaces for encoded frame, host reference, stream View,
  overlay View, and inset-adjusted content.
- One pure geometry implementation for pixel-grid mapping, aspect fitting,
  and display compatibility.
- One Android View mapper shared by native cursor rendering, direct contacts,
  absolute pointer input, and legacy absolute gestures.
- Correct point, vector, size, contact-axis, orientation, and hotspot
  transformations under translation, scale, rotation, letterboxing, zoom, and
  cutouts.
- A single window-inset owner and a pure display/window policy.

### Completed closeout work

- Audit every protocol position/size sender and every overlay consumer for
  manual offsets, ad-hoc ratios, or partial View transforms. Completed in
  `STREAM_COORDINATE_AUDIT.md`.
- Move physical-resolution/inset decisions into a tested policy model.
  Completed through `StreamDisplayGeometry`, `StreamLayoutGeometry`, and
  `WindowInsetsPolicy`.
- Document the common-parent transform invariant used by sibling stream,
  input, and overlay Views. Completed in `STREAM_COORDINATE_AUDIT.md`.
- Add a dependency rule preventing feature code from reimplementing viewport
  math. Approved consumers of `ViewCoordinateMapper` are now enforced by
  `ArchitectureBoundaryTest`.

### Exit evidence

- The repository contains no competing stream coordinate algorithm.
- Identity-transform behavior remains byte-for-byte equivalent.
- Instrumentation covers transformed siblings, system bars, cutouts, rotation,
  zoom, and both absolute and relative modes.
- Local cursor drawing and the submitted canonical pointer position use the same
  mapped value.

## Phase 5 — Typed settings and persistence

**Status:** in progress.

### Problems to remove

- `PreferenceConfiguration` is a mutable, application-wide property bag.
- Feature code reads `SharedPreferences` directly and repeatedly, including
  input, audio, controller, rendering, and UI code.
- String keys, defaults, parsing, migration, validation, and runtime policy are
  mixed.
- `StreamSettings` combines schema, rendering, dependency logic, and mutation.

### Deliverables

- Immutable, domain-scoped settings models such as stream video, input,
  controller, transfer, audio, UI, and host defaults.
- A `SettingsRepository` contract with one Android persistence adapter.
- Typed keys/codecs, centralized validation, schema versioning, and idempotent
  migration from every currently supported stored value.
- An immutable per-session settings snapshot; hot paths never reread storage.
- Settings-screen state holders that expose immutable rows/sections and accept
  typed intents.
- Explicit dependency rules for conditional visibility and compatibility
  rather than listeners mutating neighboring preferences.

### Completed slices

- Stream display/resolution and decoder snapshots.
- Input/gesture, physical-controller, and virtual-control snapshots with
  explicit live-update state owners.
- USB driver enablement and claim policy come from the stream-owned controller
  snapshot; audio-haptics routing comes from the stream-owned audio snapshot.
  The bound service performs no preference I/O and receives both states before
  device enumeration.
- Playback, mute, effects, channel layout, audio-haptics processing, controller
  rumble suppression, and USB/Kishi routing share one immutable
  `StreamAudioSettings` snapshot. PCM callbacks no longer read preferences,
  and live menu changes publish one replacement state.
- Microphone protocol-v1 capture invariants are represented by immutable
  `MicrophoneUplinkConfig`. A platform-independent lifecycle controller owns
  start/stop/error transitions, `NvConnection` depends on its session-factory
  port, and the composition root injects the Android/common-c capture adapter.
- Clipboard capability enablement and the persisted document-tree destination
  are decoded together as `TransferSettings`; stream composition and the
  pull-to-device UI no longer use the legacy preference bag or direct default
  preferences for those decisions.
- The live touch-sensitivity menu consumes immutable input/controller snapshots
  and emits schema-normalized typed intents. Mouse-wheel distance now belongs
  to `InputSettings`, and its runtime sender reads the same atomic state that
  the menu updates; the corresponding legacy property-bag field is removed.
- The device settings menu emits typed controller intents. USB claim policy,
  device/gyro/rumble options, rumble-trigger linkage, and every DualSense
  adaptive-trigger parameter now share the stream-owned
  `ControllerSettingsState`; their obsolete legacy property-bag fields and
  direct preference access are removed.
- The miscellaneous in-stream settings menu consumes immutable UI, input,
  controller, and audio snapshots and emits typed domain intents. Floating
  controls report settled positions without owning persistence policy, while
  performance overlays consume a typed projection rather than the legacy
  application-wide settings bag.
- Both display-settings entry points consume one immutable video/audio
  snapshot and emit typed intents. Immediate options update only their owned
  canonical key, while resolution, FPS, bitrate, orientation, and
  external-display mode are committed as one explicit Apply transaction.
  Custom resolutions cross a repository port.
- Versioned legacy preference migration through schema version 4.
- The in-stream action catalog and virtual-overlay buttons consume typed UI
  policy plus read-only controller visibility. Persisted startup defaults are
  no longer mutated to represent picture-in-picture or menu presentation, and
  compact-window orientation receives an immutable typed projection instead
  of the legacy application-wide settings bag.
- First-page stream-menu customization stores only bounded stable card
  references behind `GameMenuCardLayoutRepository`. Display and editor code
  are pure with respect to persistence; schema version 4 migrates and removes
  the former action-only order/hidden keys without retaining a runtime
  compatibility branch.
- Stream-menu shortcut payloads use an immutable, bounded domain document
  behind `GameMenuShortcutRepository`. The Activity-owned Android adapter
  performs a one-time import of both historical named preference sources,
  preserves stable card references and deterministic order, removes the old
  values only after the canonical write commits, and never retains a runtime
  dual-read fallback. Shortcut Fragments and catalogs no longer parse JSON or
  address persistence.
- A platform-independent virtual-control layout identity/repository contract,
  Android atomic-file adapter, bounded and validated import, and one shared
  runtime/settings I/O path. Historical file names remain an adapter-only
  compatibility detail. The unreachable parallel keyboard-element list and
  its named preference store are removed; element identity now belongs to the
  layout domain.
- Stream composition no longer creates or mutates
  `PreferenceConfiguration`. Existing input, controller, audio, transfer,
  UI, video, display, and decoder snapshots now cover every `Game` launch and
  runtime decision, including cursor modes, adaptive input throttling,
  picture-in-picture, warning/toast policy, stretch/cutout/native geometry,
  SOPS, virtual display, full-range output, and refresh-rate reduction. Direct
  display/decoder loaders replace and delete the temporary legacy adapter.
  Display-dependent capped-FPS fallback is a pure decision that leaves stored
  settings immutable, while Android-only compatibility defaults remain in
  the Android adapter package.
- The application-wide `PreferenceConfiguration` property bag is deleted.
  Application language, theme, icon density, host-list label, and optional
  background form one immutable `AppPresentationSettings` snapshot; device-
  specific icon defaults and locale migration remain Android adapters.
  Accessibility key diagnostics and GameManager integration were added to
  their existing input and stream-UI domains. Settings-only default bitrate,
  display-shape, HDR compatibility, and decoder-crash reset behavior now use
  explicit policies rather than static methods on a cross-domain bag. Host
  and app grids receive only the presentation value they consume, and the
  duplicated background renderer has one Android presenter.

### Migration order

1. Inventory every persisted key, default, writer, reader, and sensitivity.
2. Add round-trip and legacy-migration fixtures using real preference files.
3. Introduce read-only typed snapshots beside current storage.
4. Migrate runtime consumers by domain.
5. Move UI writers through the repository.
6. Remove direct default-preference reads outside the adapter and platform-only
   preference widgets.
7. Split the settings UI after the domain model is stable.

### Exit evidence

- Invalid or legacy values cannot crash settings or stream startup.
- A setting has one canonical default, parser, validator, and writer.
- Runtime components receive settings in constructors or explicit updates.
- Preference migration is repeatable and preserves paired-host/user data.

## Phase 6 — Runtime composition and monolith decomposition

**Status:** in progress.

This phase reduces `Game`, `ControllerHandler`, `MediaCodecDecoderRenderer`, and
remaining large UI controllers by ownership, not by arbitrary line-count
targets.

### Deliverables

- `Game` becomes a lifecycle and Android-event adapter plus composition root.
- Stream preparation, window policy, render orchestration, input capture,
  controller attachment, overlay presentation, and menu actions have separate
  lifecycle-bound owners.
- `ControllerHandler` is split into device discovery/attachment, slot
  assignment, mapping, report generation, sensors, rumble, LEDs, USB/evdev, and
  virtual-controller adapters.
- Decoder capability selection, codec lifecycle, frame pacing, statistics, and
  workaround policy are separated behind render contracts.
- UI screens render immutable state and emit intents; they do not perform
  transport, storage, or device I/O.
- Constructor/factory composition replaces concrete cross-subsystem creation.

### Completed slices

- Physical display-mode selection is a pure immutable policy with JVM
  fixtures for refresh reduction, high-refresh selection, resolution
  preservation, safe output width, and invalid input. `Game` only translates
  Android modes and applies the selected mode.
- `StreamRenderSurfaceController` owns Surface callback registration,
  readiness, frame-rate hints, decoder stop preparation, and callback
  detachment. Its transition state is platform-independent and tested without
  adding a queue or thread to the render path.
- `ControllerSlotAllocator` is the sole owner of the protocol's sixteen player
  slots and the initial-to-active mask transition. Android/USB device code asks
  for or releases a slot instead of mutating duplicate bitmasks.
- `ControllerSlotLease` owns each context's selected number, allocator
  reservation, and host-announcement state. Selection must precede assignment,
  cannot be silently changed, and releases are idempotent. Device recreation
  transfers reservation ownership without changing the host-visible slot,
  while the destroyed context retains a read-only routing snapshot for a
  callback already in flight.
- `ControllerAssignmentPolicy` selects fixed player one, allocator-backed
  reservation, or split-device association from immutable controller facts.
  `ControllerAssociationPolicy` separately owns the split-device identity
  rule: the candidate must be a joystick with a different name and the same
  descriptor. Android still probes the forward adjacent device before the
  reverse device and performs context creation, recursion, and slot sharing at
  the platform edge.
- `ControllerBatteryReport` owns Android-to-protocol battery conversion and
  duplicate-sample suppression. The Android adapter only samples platform or
  SHIELD APIs and sends the immutable report.
- `ControllerButtonMapper` owns the Android key-to-protocol capability table,
  vendor key-layout corrections, and face-button swapping as an immutable
  policy. `ControllerButtonMappingState` separately owns the per-session
  learning that disables Start/Select fallbacks after real buttons appear.
  Device discovery builds the policy once; the event adapter performs no
  mapping allocation or device-policy branching.
- `ControllerChordEmulationState` owns the allocation-free per-session state
  for Start/LB Select emulation, Select/LB clickpad emulation, Start/Select or
  Start/RB Mode emulation, the bumper-release grace window, and deferred stream
  exit. Android key events update the base packet and pass its mask through the
  state owner; chord flags and timing no longer leak into the device context.
- `ControllerDigitalButtonMapping` is the single immutable mapping from
  remapped Android key/scan codes to protocol mask or trigger targets. Press
  and release share this table, including diagonal D-pad and paddle mappings;
  Hat-axis and analog-trigger ownership are applied synchronously before the
  existing context state changes, with no event-path allocation.
- `ControllerMouseModeActivationState` owns the per-session Play-button
  timestamp and the exact configured-button, held-mask, repeat, and hold-time
  policy for toggling controller mouse mode or opening the stream menu. The
  Android handler retains only the resulting UI action.
- `ControllerInputState` is the single mutable owner of one input source's
  protocol-visible buttons, sticks, triggers, Hat state, and analog-usage
  history. Physical, USB, virtual, gyro-stick, touchpad-button, chord, and
  mouse-emulation paths all update or read this boundary instead of sharing
  writable packet fields through the handler context. Trigger normalization
  and Hat transitions are platform-independent and fixture-locked; the event
  path adds no allocation, lock, scheduler, or transport hop.
- `AndroidControllerInventory` is the sole adapter for startup Android/USB
  enumeration and Android 11 virtual-device classification. `Game` and
  `ControllerHandler` consume the same inventory result instead of duplicating
  platform scans, while `ControllerInventoryMask` bounds reservations to the
  protocol's sixteen slots and `ControllerDeviceClassificationPolicy` keeps
  compatibility decisions platform-independent. The former write-only
  attached-controller cache is removed.
- `ControllerAnalogInputCombiner` owns split-device trigger and stick
  aggregation. Triggers are compared as unsigned protocol values and axes by
  signed magnitude; the previous bitwise-OR corruption path has been removed.
- `ControllerMouseEmulationTranslator` owns the stateful controller-to-desktop
  button, key, stick, scroll, and trigger transitions. Each controller context
  keeps one translator and one reusable work vector; chord definitions are
  immutable, and scheduled mouse reports add no allocation or asynchronous
  hop.
- `ControllerMouseEmulationSession` owns activation, delayed reports,
  cancellation, late-callback rejection, context migration, and destruction.
  Android's main-thread `Handler` is an adapter; input-device recreation now
  restores an active session together with its scheduler instead of copying a
  stale boolean.
- `ControllerArrivalReport` derives the immutable protocol type, supported
  buttons, capability mask, and clickpad-emulation requirement from sampled
  hardware facts. Android probing and transport emission remain adapters;
  protocol policy no longer lives in the mutable device context.
- `AndroidControllerArrivalProbe` is the one-shot platform adapter for key,
  hat-axis, touchpad/clickpad, vibrator, light, sensor, and SHIELD-extension
  capability sampling. `ControllerTypeResolver` owns known vendor families and
  the native fallback contract. The device context now only supplies immutable
  runtime facts, emits the resulting report, and starts battery monitoring.
- `ControllerHapticsPolicy` resolves audio-haptics ownership versus ordinary
  rumble, including selective Kishi suppression. `ControllerRumbleAmplitudes`
  owns unsigned protocol conversion, motor ordering, single-motor mixing, and
  fallback scaling; Android classes only submit the resulting effects.
- `ControllerMotionSession` owns requested accelerometer/gyroscope rates,
  bounded sampling policy, delayed restoration, cancellation generations,
  neutral-gyro output, migration, and destruction. Physical and on-device
  fallback sensors now use the same lifecycle; `SensorManager` registration is
  a narrow platform adapter and no duplicate virtual-controller path remains.
- `ControllerMotionRegistrations` is the sole owner of active accelerometer
  and gyroscope registration leases. Manager replacement, unregister-before-
  register ordering, missing sensors, rejected registration, sampling-period
  conversion, and neutral output are deterministic under generic JVM fixtures;
  `AndroidControllerMotionBackend` contains the remaining typed platform calls.
- `ControllerMotionSensorPolicy` owns the Android-version, vendor, and setting
  gate that protects input-device sensor-manager creation. The thin
  `AndroidControllerMotionSource` touches Android's controller sensor API only
  when permitted and returns a manager only when accelerometer or gyroscope
  hardware is present.
- `ControllerMotionSampleTransformer` owns duplicate suppression, four-way
  device-orientation correction, raw samples, and protocol unit conversion in
  one allocation-free callback object. `ControllerGyroStickTranslator` owns the
  customized deadzone, response curve, smoothing, inversion, and clamp state;
  force-gyro trigger state is isolated by protocol slot instead of shared
  across all attached controllers.
- `ControllerMotionEventProcessor` owns the allocation-free realtime branch
  from raw samples through duplicate suppression and typed-settings lookup to
  either native motion output or force-gyro right-stick output. Conditional
  display-rotation reads, the left-trigger threshold, accelerometer suppression,
  translator reset, and output ordering are explicit JVM fixtures; Android's
  listener now only forwards the three sensor values.
- `ControllerAxisProfile` resolves immutable stick, trigger, and hat-axis
  assignments from platform-probed axis pairs. Dedicated, brake/gas,
  brake/throttle, RX/RY, Z/RZ, old DualShock, Linux DualShock, unnamed-device,
  and incomplete-hat cases are explicit policy fixtures; Android integer-axis
  constants remain in the device adapter.
- `AndroidControllerAxisProbe` is that single device adapter: it owns
  joystick-to-gamepad range fallback, axis-pair sampling, Sony button-C
  detection, profile construction, and Android-axis conversion. Enumeration,
  Back-button classification, context setup, deadzone sampling, and arrival
  reporting now consume the same capability view.
- `AndroidControllerDeviceProfileProbe` samples identity, key capabilities,
  touchpad ranges, axes, deadzones, and device quirks exactly once when a
  physical controller context is created. It produces an immutable
  `AndroidControllerDeviceProfile`; context construction only binds that
  profile to lifecycle resources and creates the per-session button-mapping
  state. Trigger deadzone correction is an explicit pure policy with boundary
  fixtures rather than an ordered mutation in the Android context.
- `ControllerDeviceQuirks` applies known ADT-1, ASUS, NVIDIA, Razer Serval,
  Xbox Bluetooth, and Thrustmaster key-layout corrections to immutable sampled
  facts. Button remapping consumes the resulting profile instead of depending
  on mutation order inside device-context construction.
- `ControllerExternalDevicePolicy` owns product-specific external/internal
  overrides, while `AndroidInputDeviceClassifier` is the only adapter for the
  public or legacy-reflective Android classification API. Unknown legacy
  results fail safely to external, and one sampled classification is reused by
  context setup and Back-button policy.
- `ControllerBackButtonPolicy` owns the explicit three-way decision to handle
  Back as controller input, leave it to Android navigation, or inspect the
  internal-device inventory. `AndroidControllerBackButtonProbe` performs that
  inventory scan only when requested, preserving the Serval, axisless remote,
  ordinary external accessory, GPD-style Select, and SHIELD Portable rules.
  Shared Android source-bit queries live in
  `AndroidControllerInputCapabilities` instead of the handler.
- `StreamInputLifecycleController` is the Activity-independent owner of input
  resume/pause, finishing shutdown, early motion-routing detachment, late
  controller-resource teardown, and the keyboard-listener lease. The explicit
  two-stage destroy preserves media/audio-haptics ordering while making
  duplicate and partial lifecycle callbacks deterministic.
- `UsbDriverSessionController` owns the Activity-to-service binding lease,
  including accepted-but-not-yet-connected teardown, reconnects, endpoint
  replacement, and callback revocation before controller destruction.
  `UsbDriverCallbackRegistry` publishes one atomic callback snapshot and uses
  generation-scoped leases, so an overlapping old Activity cannot clear the
  callbacks of a newer stream session. The Binder exposes one session-level
  attach/detach contract rather than independently mutable settings, listener,
  state-listener, start, and stop calls.
- `AndroidDecoderDiscovery` owns platform codec enumeration, AVC fallback,
  HEVC/AV1 allow-list adaptation, cross-codec performance comparison, and the
  immutable capability profile produced before renderer startup.
  `DecoderPerformanceEvaluator` preserves and tests Android's authoritative
  evidence order: Q performance points, then M achievable frame rates only
  when those points are unavailable, then the non-performance size/rate API.
  `MediaCodecDecoderRenderer` consumes the frozen selection and no longer mixes
  device discovery with codec execution lifecycle.
- `ControllerVibrationRenderer` owns Android vibration-target selection and
  rendering across input-device managers, Shield extensions, legacy
  vibrators, and handset fallback. Each input context now holds one target
  instead of separately mutable manager, vibrator, channel-layout, and four
  motor fields. `SingleVibratorRumblePlan` freezes zero/cancel, optional stop
  pulse, forced-strong, amplitude, and legacy PWM decisions before platform
  effects are submitted.
- `RazerKishiHapticsController` owns the optional Kishi USB sidecars as a
  synchronized lifecycle: candidate discovery, permission/open/start,
  refresh throttling, frame submission, stale replacement, policy shutdown,
  and terminal destruction. Android USB and diagnostic logging are injected
  adapters, allowing deterministic fixtures without putting test hooks in the
  production device class.
- `ControllerBatterySession` owns periodic battery reporting for each physical
  controller. Enable, disable, device migration, and terminal destruction use
  generation-scoped callbacks, so a canceled or late tick cannot restart
  polling. Live typed-settings updates now apply immediately to existing
  controllers instead of taking effect only after device reattachment.
- `AndroidControllerBatterySource` owns Android S and SHIELD-extension
  sampling, while `ShieldControllerBatteryPolicy` converts proprietary
  connection/charging facts into tested Android battery semantics.
  `ControllerBatteryReporter` owns duplicate suppression and protocol output;
  the scheduler no longer reaches through `ControllerHandler` into platform
  APIs or stores reporting history in the mutable device context.
- `ControllerLedSession` owns desired controller-light state, while
  `AndroidControllerLedTarget` owns Android 12+ light discovery, lazy session
  creation, request rendering, and idempotent close. Input-device recreation
  now closes the old device's hardware session and reapplies only the desired
  color through a newly bound target instead of transferring a stale platform
  handle between device identities.
- `DecoderSelectionPolicy` owns HEVC/AV1 acceptance decisions and stream color
  defaults. The MediaCodec adapter performs discovery and capability queries,
  and no longer carries the unused metered-network parameter or unreachable
  AV1 fallback branches.
- `DecoderCapabilityProfile` freezes direct-submit, per-codec reference-frame
  invalidation, crash fallback, and slice preferences before setup. Capability
  reporting and active-codec selection read one immutable snapshot.
- `DecoderStatisticsTracker` owns active, previous, and cumulative decoder
  windows plus all per-frame counters. The realtime threads perform lock-free
  scalar updates; only the optional one-second overlay snapshot allocates.
- `CodecRecoveryCoordinator` owns recovery priority, attempt budget, and
  three-thread quiescence state. The renderer retains the monitor and MediaCodec
  operations, while atomic promotion rules are deterministic under JVM tests.

### Exit evidence

- No extracted controller retains an Activity beyond its lifecycle scope.
- Each subsystem can be constructed with fakes in a JVM or focused Android
  test.
- Device attach/detach, focus loss, configuration change, stream restart, and
  process recreation have deterministic tests.
- Splitting does not add queues or asynchronous hops to realtime input/audio/
  video paths.

## Phase 7 — Hosts, discovery, pairing, and credentials

**Status:** pending.

### Deliverables

- Immutable host identity and connection models that distinguish stable host ID,
  advertised endpoints, user aliases, reachability, and pairing state.
- Separate discovery sources, reachability probing, repository, pairing use
  case, and UI presentation.
- Serialized, cancelable refresh with explicit merge/conflict policy.
- A single credential/certificate owner with atomic persistence and rollback.
- Pairing and launch state machines that reject stale callbacks and duplicate
  operations.
- Database migration and backup/restore tests using existing paired-host data.

### Exit evidence

- Discovery cannot overwrite identity or credentials using a transient address.
- Manual, LAN, and remote endpoints converge on one host record deterministically.
- App restart, endpoint change, failed pairing, and concurrent refresh preserve
  valid host data.
- Sensitive material is not logged and is written atomically.

## Phase 8 — Cross-client transfer and microphone contracts

**Status:** pending.

This phase covers Android, Moonlight Qt, Sunshine, and the shared common-c
protocol together. It is complete only when capability semantics and fixtures
agree across all participants.

### Deliverables

- Versioned capability negotiation with no unused legacy clipboard branch.
- One clipboard capability switch covering text, images, files, and folders.
- Metadata-only copy notification; payload enumeration and transfer begin only
  on explicit remote paste, pull, or share.
- Symmetric request/offer/job state machines, request IDs, cancellation,
  timeout, retry classification, integrity checks, and bounded backpressure.
- Sandboxed path handling, atomic destination creation, conflict policy, and no
  transport-private folder exposed on the desktop.
- Microphone negotiation and packetization over the existing audio transport,
  with explicit format/channel conversion and bounded callback work.
- Shared protocol fixtures and failure-injection tests across Android, Qt,
  Sunshine, and common-c.

### Exit evidence

- Copying a large tree performs bounded metadata work and cannot stall input.
- Transfer begins only when the destination asks for the payload.
- Android-to-host, host-to-Android, Qt-to-host, and host-to-Qt pass the same
  text/image/file/folder matrix.
- Interrupted and repeated operations cannot publish a partial result as
  complete.
- Mono and stereo microphone input produce the negotiated host format without
  pitch, speed, or channel corruption.

## Phase 9 — Physical Gradle module boundaries

**Status:** pending.

### Preconditions

- Package dependency checks show an acyclic graph.
- Public contracts have remained stable through at least one completed domain
  migration.
- The proposed boundary improves ownership, test isolation, reuse, or build
  enforcement.

### Deliverables

- A minimal module graph derived from measured package dependencies. Candidate
  boundaries are `protocol`, `stream`, `input`, `render`, `settings`, `hosts`,
  `transfer`, and `app`; candidates are merged when separation creates
  boilerplate without enforcement value.
- Internal-by-default APIs and minimal exported contracts.
- Module-local tests, fixtures, resources, and lint configuration.
- Dependency verification, version catalog/build-logic consolidation, and
  reproducible Java/JNI builds.
- Explicit ownership for common-c revision and native artifacts.

### Exit evidence

- Gradle rejects every forbidden dependency previously guarded only by tests.
- No module depends cyclically on `app` or a feature UI.
- Debug/release and all product flavors build from a clean checkout.
- Moduleization does not regress incremental build time without a documented
  compensating benefit.

## Phase 10 — Debt removal and release hardening

**Status:** pending.

### Deliverables

- Delete obsolete Axixi/GameSbs code, temporary adapters, duplicate resources,
  unused settings and migrations, dead protocol branches, test-only production
  hooks, and stale logging.
- Resolve deprecation, nullability, lifecycle, lint, resource, JNI, and Gradle
  warnings; each remaining suppression is narrow and justified.
- Keep Android release unobfuscated unless a separately approved release policy
  changes that product requirement.
- Release logging is bounded and privacy-safe; diagnostic tracing is explicit,
  temporary, and excluded from normal hot paths.
- Dependency/SBOM, license, secret, certificate, exported-component, path,
  permission, and network-security review.
- Crash-free upgrade/rollback rehearsal and documented operational runbook.

### Exit evidence

- `verifyLocal`, lint, unit tests, root/non-root instrumentation, release
  assembly, install, upgrade, and launch all pass from a clean checkout.
- The designated Android devices and Windows host pass the complete input,
  render, controller, transfer, microphone, and lifecycle matrix.
- Release has no unexplained warning, ignored failure, hidden compatibility
  path, or dirty generated output.
- All three client/server repositories and shared common-c point to committed,
  pushed, mutually compatible revisions.

## Mandatory verification gates

### Per commit

- Focused unit/component tests for the touched behavior.
- Both product flavors compile.
- Architecture-boundary tests.
- `git diff --check`.
- No unrelated user changes included.

### Per phase

- Full `verifyLocal`.
- Root and non-root connected instrumentation.
- Release build and clean/upgrade installation.
- Domain-specific protocol or hardware matrix.
- Cancellation, destruction, and late-callback tests.
- Before/after performance comparison for a hot-path change.

### Final release

| Area | Required evidence |
| --- | --- |
| Input | 120 Hz device matrix; p99 client processing below 1 ms; no new steady-state MOVE allocation; no stuck remote state |
| Rendering | Surface/codec/HDR lifecycle, rotation, cutout, PiP, background/foreground, and decoder fallback |
| Controllers | USB/Bluetooth/evdev, slots, reconnect, sensors, rumble, virtual controls, and unsupported devices |
| Hosts | discovery, manual/remote endpoints, pairing failure/retry, certificate persistence, upgrade migration |
| Transfer | text/image/file/folder, large trees, empty files, Unicode, conflicts, cancel/retry, both directions and clients |
| Microphone | mono/stereo, negotiated rates/formats, mute/restart, long capture, and callback overload |
| Operations | bounded release logs, crash diagnostics, no sensitive payloads or credentials, reproducible artifacts |

## Quality metrics

Metrics are guardrails, not targets to game:

- zero known forbidden package dependencies;
- zero direct preference reads in migrated runtime domains;
- zero duplicate production owners for a migrated state;
- zero unbounded queues in input, audio, video, or transfer;
- zero unexplained release warnings;
- every temporary adapter has an owner and removal phase;
- every lifecycle owner has idempotent cancellation/destruction tests;
- every cross-client protocol change has fixtures on all participating projects.

Class size, method count, test coverage percentage, and module count are
diagnostic signals. They do not justify superficial splitting, low-value tests,
or abstraction without a stable responsibility boundary.

## Definition of complete

The refactor is complete only when all ten phases meet their exit evidence, the
legacy path has been deleted rather than hidden, all affected repositories are
committed and pushed, release artifacts pass the final device/host matrix, and
the architecture rules prevent reintroducing the removed coupling.
