# Settings Architecture

## Ownership

`SettingsRepository` is the persistence boundary. A `SettingKey<T>` owns the
stored name, storage type, canonical default, and validation for one value. The
Android adapter maps those keys to default `SharedPreferences`.

Runtime components never receive a repository or `SharedPreferences` merely to
query policy at arbitrary times. The app composition root reads once and builds
immutable domain snapshots:

```text
SharedPreferences
    -> SharedPreferencesSettingsRepository
        -> typed keys and versioned migrations
            -> immutable domain settings snapshots
                -> runtime controllers
```

Settings UI writes typed keys or domain update intents through the repository.
It does not duplicate parsing or defaults.

## Invalid and legacy data

- A missing key resolves to its canonical default.
- A stored value of the wrong primitive type resolves to the canonical default
  and never throws into Activity creation.
- Numeric constraints are applied by the key schema on reads and writes.
- String-backed enumerations parse into explicit domain enums.
- Legacy transformations are versioned, ordered, idempotent, and committed as
  one batch before a snapshot is published.
- Downgrade/forward-compatibility behavior for an unknown enum string is
  explicit in the domain codec; it is never an accidental `else` branch.

The resolution aggregate is the first migrated compound setting. Resolution,
selection mode, aspect policy, and FPS are decoded together by
`StreamResolutionCodec`; `StreamResolutionSettingsLoader` replaces the former
scattered parsing and commits all repairs in one editor transaction. Valid
custom dimensions retain their mode when only textual canonicalization is
required, while named legacy presets and malformed values follow explicit,
tested migration rules.

`SettingsMigrationRunner` owns the ordered schema version. Version 1 removes
the old 5.1-audio, never-drop-frames, and image-only clipboard switches in one
transaction while preserving their user-visible choices. It also detects
legacy values reintroduced by a downgrade without ever reducing a newer stored
schema version. Version 2 repairs the invalid historical on-screen gamepad
layout default to the first valid layout identifier. Version 3 migrates the
legacy Mbps bitrate key to the canonical Kbps key without overwriting a value
already written by a newer build, then removes the obsolete key. Version 4
converts the former comma-separated stream-menu action order and hidden-action
set into the canonical card-reference document. It preserves an existing v2
card layout and removes both v1 keys, leaving no runtime compatibility branch.
Version 5 replaces every historical widget-shaped persistence name with a
domain-shaped dotted name. Each `SettingKey` declares its former names as
migration-only aliases; `SettingsKeyCatalog` validates that aliases are unique
and cannot collide with canonical names. The canonical value always wins when
both exist. Otherwise the first present alias is normalized through the same
typed schema, copied, and all aliases are removed in the single versioned
transaction. Late aliases written by a downgraded build are cleaned without
downgrading a future schema version.

## Settings-screen information architecture

The screen is organized by user task rather than by feature origin or author:
video and display, audio, touch and mouse, gamepad, haptics, virtual controls,
clipboard and files, stream interface, app appearance, system and
accessibility, backup and restore, and about. No category represents a fork,
private build, or implementation layer.

Settings-screen metadata has its own boundary. `SettingsRegistry` converts the
Android XML document into `SettingsSection` and `SettingsItem` models and
rejects any persisted row whose declared widget type disagrees with its typed
schema. `SettingsStore` alone adapts those models to `SettingsRepository`.
Dependency evaluation, selected-entry lookup, slider normalization and value
formatting operate through the read-only `SettingsValueReader` contract, so
they are tested without an Activity or Android preferences. The Activity owns
only rendering, system capability discovery, dialogs, and user intents.

Persisted rows use only canonical `SettingKey` names. Sections, actions, and
non-persisted editors use identifiers from `SettingsScreenIds`; they can never
be mistaken for stored values. `preferences.xml` is presentation metadata,
while `SettingsScreenKeyCatalog` remains the executable contract for storage
type, validation, and default. Section icons are selected from stable section
IDs rather than translated titles or legacy-key substrings.

Document-backed actions form a separate Android boundary. A pure
`SettingsDocumentActionRouter` maps stable, non-persisted row IDs to explicit
actions. `SettingsDocumentController` then owns Storage Access Framework
requests, persisted directory grants, virtual-control layout exchange, host
database import, credentials, accessibility configuration, and background
selection for the Activity lifecycle. `StreamSettings` only forwards the user
intent and provider result; it has no file, provider, or database I/O branch.

Conditional visibility is a policy result, not an Activity-side collection of
SDK checks. `AndroidSettingsDeviceCapabilities` converts platform features,
sensors, USB support, picture-in-picture support, and vibration facilities into
an immutable semantic snapshot. `SettingsVisibilityPolicy` combines that
snapshot with the typed barometer-mode value and returns only stable section
and item IDs to hide. The pure policy contains no Android dependency and is
covered as a complete decision table.

`SettingsScreenModel` owns the mutable presentation graph produced by the
registry. Stable-ID lookup, dependency binding, structural hide operations,
empty-section filtering, and selected-section bounds all pass through this one
model. The Activity renders the same observed list and cannot maintain a
parallel structural state or silently ignore an unresolved dependency.

Display capability discovery follows the same adapter-policy-application
split. `AndroidSettingsDisplayCapabilities` is the only settings component
that touches Display modes, cutouts, decoder capabilities, or Android HDR
types. `SettingsDisplayPolicy` converts its immutable result into ordered and
deduplicated native resolution options, explicit preset fallbacks, frame-rate
removals, the optional native frame-rate row, and a semantic HDR state.
`SettingsDisplayController` applies that plan to `SettingsScreenModel` and
typed storage. Resolution selection identifies canonical presets through
`StreamResolutionCodec`; it does not infer meaning from mutable row indexes.

## Snapshot lifecycle

A stream session receives one snapshot during composition. A setting that
requires stream restart changes the next snapshot, not the active session.
Settings that are intentionally live-updateable use a typed controller method
and publish a replacement immutable value on the controller's owning execution
domain.

No input, audio, video, controller-report, or transfer packet callback may read
persistent settings.

The stream touch-settings dialog receives immutable `InputSettings` and
`ControllerSettings` snapshots. Each user action emits a schema-normalized
domain update object; the composition root applies it to the current snapshot,
persists only its owned canonical keys, and publishes the replacement. Reset is
one multi-key input intent, so state and persistence cannot observe a partially
reset sensitivity configuration. Programmatic seek-bar rendering never emits
updates.

The device settings dialog follows the same contract. Its historical
bind-all-USB action is represented as one compound intent: enabling it also
enables the USB driver and persists both canonical keys in one editor
transaction, while disabling it preserves the already-enabled driver exactly
as before. DualSense mode and parameter ranges are validated by the schema;
preview edits, rumble linkage, and the explicit apply action all observe the
same `ControllerSettingsState`.

## Migration ledger

| Domain | Typed snapshot | Runtime storage reads removed | Legacy adapter removed |
| --- | --- | --- | --- |
| Stream display/window | `StreamDisplaySettings`; `StreamVideoSettings` with one immutable active-session snapshot plus an atomic settings-UI state | `Game` receives dimensions, native/stretch/cutout/external-display/HDR/gravity, virtual-display mode, display-mode enforcement, and SOPS launch policy from typed composition; both display-dialog entry points consume one typed snapshot and emit typed intents | The temporary `LegacyPreferenceSettingsAdapter` is deleted; generic settings-screen rows still need typed intents |
| Stream video/decoder | `StreamDecoderSettings`; typed resolution and video aggregates; pure `StreamFramePacingPolicy` | Decoder, launch, refresh-rate selection, warning thresholds, cursor scaling, and performance-statistics paths no longer receive `PreferenceConfiguration`; resolution/FPS/bitrate parsing, repair, frame-pacing fallback, and default policy have one safe codec/policy; display Apply persists its seven owned values atomically | Stream composition has no legacy adapter; remaining generic settings-screen rows still use the legacy screen implementation |
| Input and gestures | `InputSettings` with one atomic `InputSettingsState` per stream | Pointer, touchscreen/touchpad, gesture, keyboard, virtual-touchpad, mouse-wheel, native/local cursor, and adaptive transport-throttling policy no longer read storage or receive `PreferenceConfiguration`; the live touch-sensitivity and miscellaneous menus emit typed update intents and publish one replacement snapshot | Remaining generic settings rows still require typed intents |
| Physical controllers | `ControllerSettings` with one atomic `ControllerSettingsState` per stream | `ControllerHandler` and `UsbDriverService` no longer receive `PreferenceConfiguration` or reread storage from controller, sensor, rumble, battery, USB attach, or permission callbacks; touch, device, and miscellaneous menus emit typed controller intents; rumble-trigger linkage, force-gyro policy, and DualSense adaptive-trigger application consume the same live snapshot | Remaining generic settings rows still require typed intents |
| On-screen controls | `VirtualControlSettings` with one atomic `VirtualControlSettingsState` per stream | Active virtual gamepad, virtual-key, touchpad-button, and full-keyboard rendering/input paths consume typed snapshots; the virtual-key startup default and automatic-orientation policy are typed, while current overlay visibility is reported by the owning controller rather than written into persistent configuration; stream-menu writers emit immutable domain updates; named layouts use `VirtualControlLayoutRepository` rather than direct file access | Layout element DTO/codec separation from the game-menu model remains; unused named-`SharedPreferences` loader path has been removed |
| Stream audio | `StreamAudioSettings` with one atomic `StreamAudioSettingsState` per stream | Playback, mute, audio effects, and phone/controller audio-haptics consume the same typed snapshot; the miscellaneous menu emits typed live-audio intents; PCM callbacks perform no preference I/O; controller rumble suppression and USB/Kishi routing no longer duplicate audio policy inside `ControllerSettings` | Restart-only settings still use the legacy settings screen |
| Microphone | No persisted policy; protocol-v1 invariants live in immutable `MicrophoneUplinkConfig` | Capture is an injected Android adapter; the platform-independent lifecycle controller owns all start/stop/error transitions and is unit tested without `AudioRecord` or JNI | No legacy preference exists; future formats require explicit protocol negotiation rather than a hidden setting |
| Clipboard and transfer | `TransferSettings` captures clipboard enablement and the bounded persisted document-tree URI | Stream composition no longer reads the legacy preference bag for capability enablement; pull-to-device UI reads, repairs, and writes the directory through `SettingsRepository` and typed keys | Generic settings-row writers still need the typed-intent migration; clipboard loop-suppression checkpoints are operational state, not user settings, and move behind a storage port in phase 8 |
| In-stream UI | `StreamUiSettings` with one atomic `StreamUiSettingsState` per stream; immutable `GameMenuCardLayout` and `GameMenuShortcut` documents behind consumer-owned repository ports | Floating-control behavior and remembered position, compact/expanded performance presentation, interaction, scale, margin, rumble HUD, picture-in-picture, warning visibility, latency toast, Android GameManager integration, and built-in shortcut catalog policy consume one typed snapshot; card layout stores bounded stable IDs; shortcut payloads use a bounded immutable document with defensive key arrays; both repositories are Activity-owned while Fragments/catalogs perform no preference or adapter I/O; Views emit events and do not read settings storage or own persistence decisions | Generic settings-screen rows remain |
| General UI and host list | `AppPresentationSettings` | Locale, theme, icon density, optional background/blur/file, and host-list label are loaded as one immutable snapshot; host grid has no settings dependency, app grid receives only icon density, and background rendering has one Android presenter | `PreferenceConfiguration` and its adapter-level instrumentation tests are deleted; the generic settings screen still needs immutable section/row state and typed intents |

The ledger is complete only when direct default-preference reads are confined to
the repository, legacy migrations, and platform preference widgets that have
not yet emitted a typed update intent.

`Game` is a composition root, not a settings decoder. It creates the Android
repository, runs schema/device bootstrap once, and loads immutable input,
controller, audio, UI, video, display, decoder, transfer, and virtual-control
snapshots. The active stream keeps immutable launch/display/decoder values even
when a settings dialog updates the next-launch state. Display-dependent frame
pacing returns a separate effective session decision and never mutates either
the persisted value or its decoded snapshot. Android-only HDR firmware and
Android 12 controller-sensor workarounds live in the platform adapter package,
outside the pure settings domains.

Application presentation is independent from stream-session UI.
`AppPresentationSettingKeys` owns language, theme, app-icon density, host-list
label, and background policy. The platform-independent loader produces one
immutable snapshot; `AndroidAppPresentationSettingsLoader` alone computes and
persists the device-dependent small-icon default. `AndroidAppLocale` owns the
Android 13 per-app locale migration and pre-Android-13 resource override.
Host/app activities and adapters do not receive the former cross-domain
property bag.

Accessibility key logging is input diagnostics. The accessibility service
loads one typed `InputSettings` snapshot, observes only the owned canonical
key, and publishes the resulting boolean through a volatile field; key-event
callbacks perform no preference or migration I/O. Android GameManager calls
receive the session's typed UI policy. Non-streaming entry points load that
same policy through an Android adapter before resetting GameManager state.

Display dialogs resolve `GameDisplayHost` from their attached Activity rather
than receiving repositories, snapshots, or persistence callbacks through
instance-field setters. Nested resolution, FPS, and bitrate dialogs return
their typed selection to the display dialog through a Fragment target that the
platform restores with Fragment state. Consequently, process or configuration
recreation cannot leave a visible display dialog with an unbound persistence
dependency.

Audio effects, channel layout, and host-side playback are captured when a
stream is composed. Mute and audio-haptics policy are intentionally
live-updateable: the menu persists the value, the composition root loads one
validated replacement snapshot, and the audio renderer plus controller/USB
consumers observe that shared state. The PCM callback reads only the volatile
snapshot and never touches Android preferences.

The performance overlay's persisted enablement and presentation mode are
loaded into `StreamUiSettingsState` at stream composition. The in-stream
show/hide and compact/expanded actions are intentionally session-local and
replace only that runtime snapshot; settings-menu intents persist their owned
fields through `SettingsRepository`. `StreamFloatingControlController` owns
the optional View lifecycle, reads the latest action from the stream-scoped
state, and forwards settled coordinates through a narrow sink. The
`FloatingMagnetView` receives only an immutable initial-position snapshot and
never owns persistence; the composition root alone applies the typed position
update.

Persisted launch defaults and mutable session presentation are separate
concepts. `VirtualControlSettings` determines whether virtual keys are created
at stream startup, while `ControllerSettings` supplies the initial on-screen
gamepad policy. After creation, each overlay controller is the sole owner of
its current visibility and exposes a read-only query to the stream menu and
orientation composition boundary. Hiding an overlay for picture-in-picture or
toggling it from the menu never mutates a persisted-settings snapshot or the
legacy aggregate.

`StreamVirtualControlsController` owns the runtime overlay instances behind
small lifecycle ports. It lazily creates the virtual gamepad, virtual keys, and
full keyboard through one Android factory, while startup policy remains an
immutable input supplied by stream composition. Configuration refresh,
picture-in-picture hiding, menu visibility, layout-edit suppression, and
teardown all observe that one owner; neither the Activity nor the menu retains
a concrete overlay View.

`StreamOrientationRequest` is the immutable projection crossing from stream
composition into the Android orientation adapter. It combines active overlay
visibility with typed stream-video and virtual-control policy. Neither the
orientation adapter nor its pure policy can depend on
`PreferenceConfiguration`, preference widgets, or storage.

The first-page stream-menu layout is a user-owned reference document, not a
copy of commands or shortcut payloads. `GameMenuCardLayout` stores a bounded,
deduplicated order and hidden-ID set. The pure configuration policy resolves
those IDs against the current catalog, ignores removed cards, applies defaults
to newly discovered cards, and serializes no UI resources. The Fragment and
editor emit a layout save intent through `GameMenuHost`; only the Activity
composition root reaches `GameMenuCardLayoutRepository`.

Stream-menu shortcuts are a separate user-authored payload document.
`GameMenuShortcut` is immutable, defensively copies its chord arrays, and
explicitly distinguishes Moonlight protocol key codes from Android key codes.
The pure catalog combines one repository snapshot with fixed built-ins; it
cannot read preferences, parse JSON, or run migrations.

`SharedPreferencesGameMenuShortcutRepository` is the sole Android adapter for
this document. On first access it imports the historical `specialPrefs`
payload and sorted dynamic `quick_axi_keyAssemble` entries, commits one
versioned and bounded canonical document, and only then removes both legacy
sources. Imported IDs and custom IDs remain stable so existing first-page card
references survive the migration. Once the canonical document exists, legacy
values are cleanup-only and are never a fallback runtime source. Malformed
entries are isolated and counted without exposing their contents in logs. A
malformed, wrong-typed, or newer-version canonical document is read-only:
runtime rendering fails closed and edit intents cannot overwrite the stored
bytes.

The microphone uplink intentionally has no settings snapshot. Its 48 kHz mono,
960-sample/20 ms frames, 40 kbps Opus target, and four-frame capture buffer are
version-1 protocol invariants in `MicrophoneUplinkConfig`. `NvConnection`
depends only on a `MicrophoneUplinkSessionFactory`; the composition root injects
the Android/common-c adapter. This keeps capture construction out of the
connection state machine. `StreamMicrophoneController` separately owns Android
permission deferral and serialized UI toggle intent through the narrow
`MicrophoneUplinkEndpoint`; completion invalidates menu state explicitly rather
than using delayed polling. Both lifecycle layers are deterministic in plain
JVM tests.

Clipboard enablement is a restart-only stream capability. The selected Android
document tree remains an opaque, bounded string in the platform-independent
snapshot and is parsed only by the Storage Access Framework UI adapter. Missing
providers, revoked grants, and malformed URIs clear the typed key before asking
the user to select a replacement. The named `clipboard_sync_state` store is not
configuration: it is an operational loop-suppression checkpoint and is
therefore tracked for the transfer architecture phase rather than folded into
the global settings schema.

## Editable layout documents

Virtual-control layouts are user-authored documents, not settings values. The
selected keyboard/gamepad profile remains a typed setting, while the document
content crosses a separate `VirtualControlLayoutRepository` port.

- `VirtualControlLayoutKey` accepts only the five canonical profile IDs for its
  keyboard or gamepad family and an explicit orientation.
- The Android adapter alone maps that key to the historical
  `axi_<profile>[_1].txt` name, preserving existing user layouts without
  exposing a path-bearing API.
- Documents are UTF-8, bounded to 1 MiB, and written with `AtomicFile`.
- Imported documents must contain a JSON array before they replace the active
  file. A malformed existing document is isolated as an empty layout and
  reported through release-gated diagnostics rather than crashing the stream.
- Settings export/import and the runtime overlay use the same repository and
  identity mapping. The former direct `FileUriUtils` path and unused legacy
  configuration-loader persistence path no longer exist.
- The unreachable `GameListKeyBoardFragment` and its unconsumed
  `keyboard_axi_keyAssemble` element store have been deleted. Editable
  elements live only inside the active layout document. New elements obtain
  collision-resistant IDs from `VirtualControlElementIds`; already persisted
  IDs remain opaque and unchanged.
