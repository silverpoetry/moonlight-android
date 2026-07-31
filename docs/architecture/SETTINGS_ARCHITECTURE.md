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
| Stream display/FSR/window | `StreamDisplaySettings`; `StreamVideoSettings` with one atomic `StreamVideoSettingsState` per active UI/session scope | `Game` no longer reads FSR target, sharpness, HDR mode, or gravity; both display-dialog entry points consume one typed snapshot and emit typed intents | Remaining launch/window settings still use the legacy aggregate |
| Stream video/decoder | `StreamDecoderSettings`; typed resolution and video aggregates | Decoder and performance-statistics paths no longer receive `PreferenceConfiguration`; resolution/FPS/bitrate parsing, repair, and default policy have one safe codec/policy; display Apply persists its ten owned values atomically | Remaining launch-only decoder rows still use the legacy settings screen |
| Input and gestures | `InputSettings` with one atomic `InputSettingsState` per stream | Pointer, touchscreen/touchpad, gesture, keyboard, virtual-touchpad, and mouse-wheel runtime paths no longer read storage or receive `PreferenceConfiguration`; the live touch-sensitivity and miscellaneous menus emit typed update intents and publish one replacement snapshot | Remaining generic settings rows still require typed intents |
| Physical controllers | `ControllerSettings` with one atomic `ControllerSettingsState` per stream | `ControllerHandler` and `UsbDriverService` no longer receive `PreferenceConfiguration` or reread storage from controller, sensor, rumble, battery, USB attach, or permission callbacks; touch, device, and miscellaneous menus emit typed controller intents; rumble-trigger linkage, force-gyro policy, and DualSense adaptive-trigger application consume the same live snapshot | Remaining generic settings rows still require typed intents |
| On-screen controls | `VirtualControlSettings` with one atomic `VirtualControlSettingsState` per stream | Active virtual gamepad, virtual-key, touchpad-button, and full-keyboard rendering/input paths consume typed snapshots; the virtual-key startup default and automatic-orientation policy are typed, while current overlay visibility is reported by the owning controller rather than written into persistent configuration; stream-menu writers emit immutable domain updates; named layouts use `VirtualControlLayoutRepository` rather than direct file access | Layout element DTO/codec separation from the game-menu model remains; unused named-`SharedPreferences` loader path has been removed |
| Stream audio | `StreamAudioSettings` with one atomic `StreamAudioSettingsState` per stream | Playback, mute, audio effects, and phone/controller audio-haptics consume the same typed snapshot; the miscellaneous menu emits typed live-audio intents; PCM callbacks perform no preference I/O; controller rumble suppression and USB/Kishi routing no longer duplicate audio policy inside `ControllerSettings` | Restart-only settings still use the legacy settings screen |
| Microphone | No persisted policy; protocol-v1 invariants live in immutable `MicrophoneUplinkConfig` | Capture is an injected Android adapter; the platform-independent lifecycle controller owns all start/stop/error transitions and is unit tested without `AudioRecord` or JNI | No legacy preference exists; future formats require explicit protocol negotiation rather than a hidden setting |
| Clipboard and transfer | `TransferSettings` captures clipboard enablement and the bounded persisted document-tree URI | Stream composition no longer reads the legacy preference bag for capability enablement; pull-to-device UI reads, repairs, and writes the directory through `SettingsRepository` and typed keys | Generic settings-row writers still need the typed-intent migration; clipboard loop-suppression checkpoints are operational state, not user settings, and move behind a storage port in phase 8 |
| In-stream UI | `StreamUiSettings` with one atomic `StreamUiSettingsState` per stream; immutable `GameMenuCardLayout` and `GameMenuShortcut` documents behind consumer-owned repository ports | Floating-control behavior and remembered position, compact/expanded performance presentation, interaction, scale, margin, rumble HUD, and built-in shortcut catalog policy consume one typed snapshot; card layout stores bounded stable IDs; shortcut payloads use a bounded immutable document with defensive key arrays; both repositories are Activity-owned while Fragments/catalogs perform no preference or adapter I/O; the stream menu no longer receives `PreferenceConfiguration`; Views emit events and do not read settings storage or own persistence decisions | Generic app UI, host-list presentation, and settings-screen rows remain |
| General UI and host list | Pending | Pending | Pending |

The ledger is complete only when direct default-preference reads are confined to
the repository, legacy migrations, and platform preference widgets that have
not yet emitted a typed update intent.

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
fields through `SettingsRepository`. `AXFloatingMagnetView` receives an
immutable initial-position snapshot and emits settled coordinates. The
composition root alone decides whether the current remember-position policy
permits persistence.

Persisted launch defaults and mutable session presentation are separate
concepts. `VirtualControlSettings` determines whether virtual keys are created
at stream startup, while `ControllerSettings` supplies the initial on-screen
gamepad policy. After creation, each overlay controller is the sole owner of
its current visibility and exposes a read-only query to the stream menu and
orientation composition boundary. Hiding an overlay for picture-in-picture or
toggling it from the menu never mutates a persisted-settings snapshot or the
legacy aggregate.

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
connection state machine and makes every lifecycle transition deterministic in
plain JVM tests.

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
