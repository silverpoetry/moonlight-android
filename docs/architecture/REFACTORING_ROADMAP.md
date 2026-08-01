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

**Status:** complete.

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
- Schema version 5 gives every application preference a domain-shaped dotted
  key. Historical widget IDs are declared as typed, migration-only aliases;
  one idempotent transaction preserves the canonical value when both exist,
  normalizes migrated data, and deletes every old alias. The settings screen
  now has twelve task-oriented sections and separate stable IDs for sections,
  actions, and non-persisted editors. Fork/author categories, title-based icon
  inference, legacy automatic-update endpoints, and the obsolete sponsored
  credits surface are removed.
- The settings Activity no longer embeds its persistence adapter, screen-item
  model, section model, XML registry, or icon catalog. `SettingsStore` is the
  only Android persistence edge; dependency, formatting, validation, and
  section policy are independently testable, while `SettingsRegistry` is the
  one adapter that validates `preferences.xml` against the typed schema before
  the Activity renders it.
- Settings document actions resolve from stable row IDs through a pure action
  router. One lifecycle-bound Android controller owns provider launches,
  persisted tree grants, virtual-control layout exchange, host database
  import, credential import/export, and background selection. The Activity no
  longer performs file, provider, or database I/O, and canceled or incomplete
  provider results are handled without dereferencing absent data.
- Device-dependent settings visibility is evaluated from one immutable
  semantic capability snapshot. The Android adapter alone inspects SDK,
  package-manager, sensor, USB, picture-in-picture, and vibrator APIs; the pure
  policy decides which stable section and row IDs are hidden. This removes
  platform conditionals and feature-name strings from the Activity.
- `SettingsScreenModel` is the sole owner of row lookup, dependency binding,
  structural visibility, empty-section removal, and selected-section bounds.
  It mutates the registry-produced list in place so renderers observe one
  state graph rather than an Activity-maintained parallel index.
- Display-mode, cutout, decoder-width, refresh-rate, and HDR probing now live
  in one Android capability adapter. A pure policy produces ordered,
  deduplicated native resolution options, preset fallback removals, native FPS
  policy, and HDR state; a settings controller applies that result. The
  Activity no longer queries codecs or mutates resolution-entry arrays, and
  decoder capability failures degrade safely instead of crashing settings.
- Default bitrate and clipboard-directory presentation are collected as one
  immutable runtime-values snapshot. A pure screen controller applies slider
  bounds/defaults, the explicit bitrate editor default, and the directory
  summary through a localized text port; the Activity no longer resolves
  document providers or owns bitrate metadata constants.
- User-initiated boolean, integer, list, and text writes now pass through
  `SettingsMutationController`. It commits the typed value and returns one
  explicit result containing validation, native-refresh warning intent, and
  the post-change refresh/reload effect; resolution selection and exact
  Mbps-to-Kbps parsing are covered at this boundary. One lifecycle-bound
  scheduler coalesces effects and cancels them on teardown. The Activity has
  no direct settings write and creates no anonymous Handler per click.
- List, slider, and text value-editor windows now belong to one
  lifecycle-bound `SettingsDialogPresenter`. It owns dialog replacement,
  styling, input constraints, validation feedback, and teardown; the Activity
  receives only typed semantic selections and cannot construct an
  `AlertDialog` directly.
- Root, narrow, and wide settings layouts now have one lifecycle-bound
  `SettingsScreenRenderer`. It owns View construction, row refresh, scroll
  restoration, insets, and section selection while exposing only back,
  section, item, and switch intents. `StreamSettings` has no Android widget
  dependency and remains the composition and navigation boundary.
- Mutable XML metadata is frozen after capability policy into an immutable
  `SettingsScreenState`. Sections and rows expose unmodifiable copies and
  precomputed enabled, checked, and display values. The renderer has no
  dependency on registry items, the value reader, or storage, and emits only
  stable row IDs. Replacement snapshots update existing controls under a
  feedback guard, preserving the View tree and scroll position.
- `AndroidSettingsRepository` is the sole composition entry point for default
  application settings. Activities, services, and Android loaders no longer
  construct `SharedPreferencesSettingsRepository` or use
  `PreferenceManager`; the accessibility service observes its one live key
  through a lifecycle-bound typed observer. The obsolete XML default seeding
  call is removed, so schema defaults and migration remain authoritative.
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

**Status:** complete.

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
- `AndroidStreamDisplayController` now owns that Android translation and
  application boundary as well: physical-mode enumeration, window refresh
  hints, legacy refresh selection, television handling, selected phone-family
  system ownership, and render-surface geometry. Its immutable preparation
  result feeds configuration and render startup, while
  `StreamDisplayRefreshPolicy` preserves pacing-dependent reduction and device
  family decisions under JVM fixtures; `Game` no longer implements either
  algorithm.
- `AndroidStreamPictureInPictureController` owns version-specific PiP
  parameters, source bounds, labels, auto-entry application, and the guarded
  Android 8 manual-entry path. `StreamPictureInPictureState` separately owns
  connection and nested suppression state with underflow-safe JVM fixtures;
  USB permission prompts can no longer corrupt an Activity-level counter, and
  live PiP setting changes immediately revoke or reapply auto-entry while
  `Game` only forwards lifecycle and settings events.
- `AndroidStreamInputCaptureController` owns provider selection, input-grab
  application, local system-cursor preference, focus recovery, Samsung
  meta-key capture, and deterministic teardown. Its pure state preserves the
  user's cursor choice across grab transitions and makes the implicit
  cursor-toggle-to-grab transition explicit; missing Samsung APIs are an
  expected capability result instead of a printed exception. `Game` no longer
  mirrors capture/cursor flags or performs vendor reflection.
- `StreamOverlayVisibilityController` is the transition owner for PiP overlay
  hiding, connection-warning deferral, controller-sensor suspension, and
  GameManager notifications. Duplicate configuration callbacks are
  idempotent, warning changes made while hidden are restored accurately on
  exit, and late callbacks after teardown are rejected through a narrow host
  contract instead of Activity flags.
- `AndroidExternalDisplayController` owns secondary-display discovery,
  Presentation creation, StreamView parent transfer, display-removal recovery,
  and teardown. The first-non-default selection rule is fixture-locked without
  Android; the ad-hoc root-package Presentation, empty layout resource, and
  unused rounded-Surface helper are removed from `Game`.
- `StreamRenderSurfaceController` owns Surface callback registration,
  readiness, frame-rate hints, decoder stop preparation, and callback
  detachment. Its transition state is platform-independent and tested without
  adding a queue or thread to the render path.
- `StreamSessionPresentationController` owns main-thread connection-stage,
  failure, termination, warning, message, HDR, and native-cursor presentation
  policy for one session. It suppresses duplicate failures, bounds diagnostic
  publication to its lifecycle, and distinguishes owner-initiated shutdown
  from transport failure. `StreamConnectionMessages` freezes localized labels
  behind an Android adapter and preserves protocol error/port formatting under
  JVM fixtures; `Game` now supplies only narrow View, window, and resource
  operations while realtime controller feedback remains on the callback path.
- `StreamSessionConfigurationPlanner` owns the cross-domain startup document:
  final HDR eligibility, advertised codecs, forced-codec warnings, display-
  dependent pacing, initial gamepad policy, audio layout, clipboard/native-
  cursor capabilities, and every common-c launch field. The document is pure
  and JNI-free under JVM fixtures; `StreamSessionConfigurationAdapter` is the
  only step that constructs `StreamConfiguration` and maps transport audio and
  network constants, with on-device equivalence coverage. `Game` now samples
  Android decoder/display/controller facts and consumes the completed plan.
- `AndroidStreamMediaRuntimeFactory` is the concrete MediaCodec composition
  boundary. It initializes codec workarounds, constructs the decoder/resource
  owner, and publishes one immutable `StreamDecoderCapabilities` sample, so
  `Game` no longer creates or repeatedly interrogates the concrete renderer.
  `DecoderCrashTracker` separately owns one-attempt crash accounting and clean
  completion through a persistence port; the Android store preserves the
  synchronous pre-crash commit and legacy tombstone keys while Activity code
  no longer reads or writes decoder preferences.
- `AndroidStreamSessionUiEffectsHost` is the concrete boundary for session
  keep-screen-on flags, GameManager notifications, and input-grab changes.
  The tested `StreamSessionUiEffects` transition owner remains unchanged while
  `Game` no longer embeds its platform side-effect implementation.
- `StreamControllerFeedbackHost` owns callback-thread routing for controller
  vibration, linked trigger vibration, motion-state requests, controller LEDs,
  and rumble-overlay observation. Its ports are unit-tested independently and
  `Game` no longer implements the transport feedback contract.
- `AndroidStreamFailureDiagnosticsFactory` owns connectivity-probe wiring,
  main-thread result delivery, and MoonBridge fallback translation. `Game`
  receives only the presentation diagnostics port and no longer composes the
  worker implementation itself.
- `AndroidStreamLaunchReporterFactory` snapshots the launched host and app,
  owns shortcut/TV-channel service composition, and returns the tested
  session-scoped at-most-once reporter. Mutable Activity fields are no longer
  captured by its worker task.
- `AndroidStreamSystemUiController` owns fullscreen flags, legacy immersive
  visibility listening, delayed restoration, multi-window correction, and
  callback cancellation. Its restoration decision is isolated in the pure
  `StreamSystemUiVisibilityPolicy`, while transport callbacks are invalidated
  before presentation dependencies are destroyed.
- `AndroidStreamConnectingIndicator` is the sole owner of the connecting
  dialog reference, message updates, temporary cancellation suppression, and
  Activity teardown cleanup. `Game` no longer carries nullable dialog state.
- `AndroidStreamNativeCursorController` owns optional overlay attachment,
  callback-thread position serialization, stream-to-view cursor scaling, and
  teardown invalidation. `AndroidStreamHdrModeController` owns negotiated HDR
  application and Android window notification; neither presentation endpoint
  is implemented in `Game` anymore.
- `AndroidStreamConnectionWarningPresenter` owns warning text selection and
  visibility. Session callback routing is now composed only after controller
  input and overlay dependencies are initialized, eliminating the prior
  nullable-field construction hazard.
- `AndroidStreamSessionPresentationHost` composes the connecting indicator,
  warning presenter, HDR controller, native cursor controller, surface state,
  user feedback, and live settings behind the policy host contract. Only the
  Activity-owned stop and connected transitions remain narrow method actions.
- `AndroidKeyboardInputHost` owns keyboard-requested Activity actions and the
  tested 250 ms capture-toggle schedule. `AndroidInputDeviceRegistration`
  owns idempotent InputManager registration cleanup, while
  `StreamInputSuppressionHost` adapts the live UI suppression predicate.
- `StreamRenderSessionHost` owns the transactional handoff from a valid
  Surface to acquired media resources and the session controller. Accepted,
  rejected, and exceptional starts are unit-tested, including deterministic
  resource/UI rollback, while `Game` no longer implements the Surface host.
- `AndroidGameMenuController` is the sole owner of the stream-menu Fragment
  reference and its transient controller-input context. Restored Fragments,
  repeated opens, dismissal, microphone-state invalidation, Activity stop,
  and terminal destruction now share one lifecycle boundary instead of
  nullable Activity fields.
- `StreamSettingsSession` is the single transaction owner for settings changed
  while streaming. Typed intents are persisted before their immutable snapshot
  is published and runtime effects are notified; controller transition effects,
  audio/UI reconfiguration, virtual-control reload, and onscreen-rumble writes
  no longer duplicate persistence or change-detection policy in `Game`.
- `StreamGameMenuHost` terminates the menu's repositories and typed setting
  intents at a session-scoped application boundary. Restored Fragments resolve
  that host through `GameMenuHostProvider`; `Game` supplies only a platform-
  action adapter and no longer implements the menu/display storage contract.
  The existing app-list display editor retains its narrower direct host, and
  fake menu-session/platform ports fixture ordering and ownership without an
  Activity.
- `GameMenuState` is the immutable render snapshot for the menu and shortcut
  editor. Visibility, active-state, settings, layout, shortcut, microphone,
  input-readiness, and battery facts are sampled together by the application
  host; Fragments render that snapshot and emit typed intents. Sticky-battery
  broadcast access moved to `AndroidDeviceBatteryProvider`, eliminating the
  last direct device-I/O and exception-printing branch from the menu UI.
- `StreamHdrRequestPolicy` owns HDR request eligibility and user-warning
  selection from immutable settings and device facts. The Android capability
  provider samples OS, firmware, and display HDR10 support once; `Game` no
  longer queries display APIs or embeds platform-policy branches and warning
  text during media startup.
- `StreamMicrophoneController` owns permission deferral, duplicate-toggle
  suppression, serialized background start/stop work, lifecycle cancellation,
  error presentation events, and explicit menu-state invalidation. `Game`
  supplies the Android permission and UI adapters while `NvConnection`
  implements the narrow `MicrophoneUplinkEndpoint`; the menu no longer polls
  twice on fixed timers to guess whether an asynchronous transition finished.
- `AndroidStreamMicrophoneControllerFactory` now supplies that controller's
  concrete permission and localized-feedback ports, owns the permission
  request/result contract, and forwards only one menu-invalidation callback.
  `Game` no longer embeds microphone permission codes, Android-version gates,
  or toast policy.
- `StreamFloatingControlController` is the sole lifecycle owner of the
  in-stream floating control, including lazy attachment, visibility, current
  action dispatch, position events, and deterministic teardown. The View
  consumes an immutable initial snapshot and uses Android's standard click
  contract; `Game` only supplies typed action and persistence sinks. The old
  `AXFloating*` classes, custom listener, branded robot bitmap, and direct View
  plumbing in the Activity are removed, while the shared grid icon now has a
  product-neutral resource name.
- `StreamVirtualControlsController` is the sole lifecycle and visibility owner
  for the virtual gamepad, virtual keys, and full-keyboard overlays. A
  platform-independent edit-mode model and overlay ports keep the stream menu
  and `Game` independent from the concrete Android Views; the Android factory
  alone constructs them. Created overlays refresh together on configuration
  changes, hide together on lifecycle transitions, cancel delayed work on
  deterministic teardown, and cannot be recreated after destruction.
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
- Input-device recreation has one explicit session-state migration contract.
  It transfers the protocol report and analog ownership history, learned key
  mapping, in-progress chord and quit state, mouse-mode hold timing,
  mouse-emulation output history, and gyro filter before destroying the old
  platform resources. New device-profile facts may disable a fallback but
  migrated runtime state can never re-enable one. Slot, sensor, LED, battery,
  and scheduler restoration remain ordered after the state snapshot, so a
  focus or pointer-capture capability change is invisible to the host.
- `AndroidControllerTouchpadAdapter` owns the complete Android controller-
  touchpad boundary behind narrow target and protocol-output ports. A pure
  action policy distinguishes contact cancel from cancel-all, all-pointer MOVE
  frames, API-gated primary clickpad buttons, and unsupported actions. Physical
  axes use one finite, clamped normalization policy; clickpad state still
  participates in aggregated controller packets while contact forwarding can
  be delegated to Android's mouse path. No View, settings repository, or
  concrete connection is visible to the adapter.
- `UsbControllerInputAdapter` owns the USB-driver report boundary for state,
  motion, and touch. It validates finite normalized sticks, triggers, and touch
  coordinates, applies the context's deadzones and protocol quantization, and
  guarantees slot assignment and arrival reporting before a first motion or
  touch packet. Driver callbacks stay synchronous and allocation-free; the
  handler now performs only ID lookup, the live gyro setting gate, and adapter
  dispatch.
- `ControllerInputState` also owns radial deadzone evaluation and exact
  float-to-protocol stick quantization for physical, USB, and virtual input.
  The established non-renormalizing response and inverted Y mapping are shared
  by every source. `ControllerHandler` no longer exposes one mutable scratch
  vector to both the Android main thread and USB driver callbacks, removing a
  cross-execution-domain race without adding event allocation or a lock.
- `ControllerButtonReleaseSession` replaces the inherited main-thread sleep
  used to keep unusually short physical button presses host-visible. It starts
  the 25 ms minimum from the actual down-packet submission, keeps one fixed
  slot per protocol target, and schedules only the earliest deadline through
  Android's existing main looper. Repeats cannot restart the clock, duplicate
  ups cannot extend it, a new same-button down first flushes the old up, and
  context migration or destruction transfers or revokes the pending release
  exactly once. Ordinary presses remain synchronous and the input path adds no
  collection allocation, lock, unbounded queue, or worker thread.
- `ControllerAnalogInputCombiner` owns split-device trigger and stick
  aggregation. Triggers are compared as unsigned protocol values and axes by
  signed magnitude; the previous bitwise-OR corruption path has been removed.
- `ControllerInputReportAggregator` owns complete slot-level report fusion
  across Android, USB, and virtual sources. Assignment, slot, mouse-mode, and
  cross-mode-default participation are explicit source facts; button, trigger,
  and axis reduction runs in one ordered traversal before one output call. The
  aggregator is stateless and keeps every intermediate field method-local, so
  it adds no allocation, shared scratch state, lock, queue, or scheduler hop.
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
- `ControllerFeedbackRouter` owns slot-level fan-out for ordinary rumble,
  trigger motors, and controller LEDs across Android and USB targets. Rumble
  aggregation distinguishes no matching target, matched-but-unavailable
  hardware, successful delivery, and deliberate audio-haptics suppression;
  only the unavailable case can activate handset fallback. The router is a
  stateless bounded traversal with enum results and no event-path allocation,
  lock, queue, or scheduler hop. The same target contract also owns standard
  audio-haptics motor delivery, advanced-frame fan-out, and advanced-mode
  enablement; one read-only frame is passed through without copying or
  retention, while the independent Kishi sidecar keeps its dedicated lifecycle.
  Adaptive-trigger settings also traverse this typed target boundary; only the
  DualSense driver implements the capability and owns its complete USB report
  encoding, so the handler no longer inspects driver classes or sends raw
  commands.
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
- `UsbControllerLifecycleController` owns identity-safe publication and
  teardown of driver controller contexts. Same-instance add is idempotent,
  reused IDs revoke and dispose the old context before replacement, stale old-
  device removal cannot remove the replacement, preparation failure rolls
  back publication, and terminal destruction rejects late callbacks. Routing
  is always revoked before driver/resource teardown, while normal removal and
  shutdown retain their distinct host-notification semantics.
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

- Every Android controller holding an Activity is owned by `Game` and released
  in the same `onDestroy()` scope; policy/session owners accept fake ports and
  do not retain the Activity.
- Stream render, presentation, input lifecycle, USB binding, controller device
  replacement, menu state, live settings, microphone, diagnostics, and overlay
  owners are constructed under JVM fixtures or focused Android adapters.
- Surface recreation, focus/capture transitions, device attach/detach and state
  migration, repeated stream start/stop, restored menu Fragments, and terminal
  late callbacks have deterministic fixtures. Debug deep-link smoke keeps the
  full `Game` Activity alive through dependency composition, and Release
  launcher smoke runs after every full gate.
- All controller and decoder extraction is synchronous on the pre-existing
  callback path. No new queue, worker thread, lock, timer, packet allocation,
  or transport hop was added to realtime input/audio/video flow.

## Phase 7 — Hosts, discovery, pairing, and credentials

**Status:** in progress.

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

### Completed slices

- Introduced immutable, comparable values for stable host ID, advertised and
  user-facing identity, endpoint provenance, persistent host metadata, and
  transient connection state. Credentials are absent from discovery and host
  metadata types, so a probe result cannot overwrite a certificate by API
  construction.
- Added a deterministic observation merge policy: an observation updates only
  the endpoint kinds it actually reports, preserves user aliases and
  unobserved manual/remote endpoints, and rejects cross-host merges.
- Routed every legacy host probe merge through one explicit transition policy.
  Address tuples are immutable, remote-port correction creates a new value,
  partial probes preserve unobserved endpoints, and probe data cannot copy or
  replace the pinned server certificate.
- Migrated pinned certificates transactionally from the legacy host column to
  a dedicated credential table in the existing backup-compatible database.
  Discovery writes metadata only; pairing and import use explicit credential
  mutations, and a database-open failure can no longer delete paired hosts.
- Replaced the logged, pseudo-random client UID with a strictly validated,
  cryptographically generated identity stored through `AtomicFile`. Host
  diagnostics no longer emit client/host IDs, names, MAC addresses, or
  endpoints, and `ComputerDetails.toString()` is redacted by construction.
- Extracted Android mDNS binding into a lifecycle-owned discovery source.
  Discovery now publishes immutable, provenance-tagged endpoint candidates;
  it cannot manufacture stable identity, connection state, or credentials.
- Extracted host pairing from `PcView` into a transport-independent,
  serialized use case plus an `NvHTTP` adapter. Pair-state mapping is covered
  for every protocol result, certificate persistence precedes state
  invalidation, and persistence failure attempts a remote unpair while
  preserving rollback failure as suppressed evidence.
- Pairing execution now has one lifecycle-owned worker and a generation-gated
  main-thread callback. Duplicate requests are rejected before polling is
  frozen; owner destruction cancels work before the remote pairing boundary,
  suppresses stale callbacks, and deliberately lets an in-flight credential
  transaction finish without interruption.
- The legacy service adapter resolves canonical `HostId` values back to the
  exact persisted host key before writing a certificate. This preserves
  existing mixed-case host databases while keeping domain identity
  case-insensitive.
- Replaced the hand-written four-thread endpoint race with an immutable,
  physically deduplicated reachability plan, a pure priority-selection state
  machine, and one lifecycle-owned bounded executor. The original local,
  manual, remote, and IPv6 precedence plus the 200 ms upgrade window are
  frozen by fixtures; interruption cancels outstanding probes, and an
  unexpected endpoint exception completes as a failed candidate instead of
  hanging refresh forever.
- Corrected non-byte-aligned IPv4 subnet matching to compare the most
  significant prefix bits, and made reachability identity checks use canonical
  `HostId` equality with an exact-only fallback for malformed legacy records.

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
