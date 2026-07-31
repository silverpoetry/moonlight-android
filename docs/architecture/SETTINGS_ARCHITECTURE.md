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
layout default to the first valid layout identifier.

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

## Migration ledger

| Domain | Typed snapshot | Runtime storage reads removed | Legacy adapter removed |
| --- | --- | --- | --- |
| Stream display/FSR/window | `StreamDisplaySettings` | `Game` no longer reads FSR target, sharpness, HDR mode, or gravity | Pending full stream settings migration |
| Stream video/decoder | `StreamDecoderSettings`; typed resolution aggregate | Decoder and performance-statistics paths no longer receive `PreferenceConfiguration`; resolution/FPS parsing and repair have one safe codec | Pending remaining stream settings migration |
| Input and gestures | `InputSettings` with one atomic `InputSettingsState` per stream | Pointer, touchscreen/touchpad, gesture, keyboard, virtual-touchpad, and mouse-wheel runtime paths no longer read storage or receive `PreferenceConfiguration`; the live touch-sensitivity menu emits typed update intents and publishes one replacement snapshot | Remaining generic settings rows and miscellaneous stream-menu writers still require typed intents |
| Physical controllers | `ControllerSettings` with one atomic `ControllerSettingsState` per stream | `ControllerHandler` and `UsbDriverService` no longer receive `PreferenceConfiguration` or reread storage from controller, sensor, rumble, battery, USB attach, or permission callbacks; the service is configured before enumeration and observes coherent live snapshot replacements; touch-menu controller sensitivity uses the same typed update boundary | Remaining device-menu writers and adaptive-trigger settings still require typed intents |
| On-screen controls | `VirtualControlSettings` with one atomic `VirtualControlSettingsState` per stream | Active virtual gamepad, virtual-key, touchpad-button, and full-keyboard rendering/input paths consume typed snapshots; stream-menu writers emit immutable domain updates; named layouts use `VirtualControlLayoutRepository` rather than direct file access | Layout element DTO/codec separation from the game-menu model remains; unused named-`SharedPreferences` loader path has been removed |
| Stream audio | `StreamAudioSettings` with one atomic `StreamAudioSettingsState` per stream | Playback, mute, audio effects, and phone/controller audio-haptics consume the same typed snapshot; PCM callbacks perform no preference I/O; controller rumble suppression and USB/Kishi routing no longer duplicate audio policy inside `ControllerSettings` | Restart-only settings still use the legacy settings screen |
| Microphone | No persisted policy; protocol-v1 invariants live in immutable `MicrophoneUplinkConfig` | Capture is an injected Android adapter; the platform-independent lifecycle controller owns all start/stop/error transitions and is unit tested without `AudioRecord` or JNI | No legacy preference exists; future formats require explicit protocol negotiation rather than a hidden setting |
| Clipboard and transfer | `TransferSettings` captures clipboard enablement and the bounded persisted document-tree URI | Stream composition no longer reads the legacy preference bag for capability enablement; pull-to-device UI reads, repairs, and writes the directory through `SettingsRepository` and typed keys | Generic settings-row writers still need the typed-intent migration; clipboard loop-suppression checkpoints are operational state, not user settings, and move behind a storage port in phase 8 |
| General UI and host list | Pending | Pending | Pending |

The ledger is complete only when direct default-preference reads are confined to
the repository, legacy migrations, and platform preference widgets that have
not yet emitted a typed update intent.

Audio effects, channel layout, and host-side playback are captured when a
stream is composed. Mute and audio-haptics policy are intentionally
live-updateable: the menu persists the value, the composition root loads one
validated replacement snapshot, and the audio renderer plus controller/USB
consumers observe that shared state. The PCM callback reads only the volatile
snapshot and never touches Android preferences.

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
